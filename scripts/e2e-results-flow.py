#!/usr/bin/env python3
"""E2E HTTP P1→P4: tiếp nhận → mã hóa → chuyển mẫu → Phiếu 5 → hồ sơ kết quả → tra cứu public.
Tham số: E2E_BASE, E2E_ADMIN_EMAIL, E2E_ADMIN_PASSWORD; KEEP=1 để giữ hồ sơ đã phát hành."""
import json, sys, uuid, urllib.parse, http.cookiejar, urllib.request, urllib.error, io

import os
# Chạy trên backend CÔ LẬP (MySQL/Redis tạm), KHÔNG chạy trên database làm việc: script tạo user và dữ liệu thử.
BASE = os.environ.get("E2E_BASE", "http://127.0.0.1:18080/api/v1")
ADMIN_EMAIL = os.environ["E2E_ADMIN_EMAIL"]
ADMIN_PASSWORD = os.environ["E2E_ADMIN_PASSWORD"]
results = []

class Client:
    def __init__(self, name):
        self.name = name
        self.jar = http.cookiejar.CookieJar()
        self.op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.call("GET", "/auth/csrf")
    def csrf(self):
        for c in self.jar:
            if c.name == "XSRF-TOKEN": return c.value
    def call(self, method, path, body=None, raw=None, ctype=None, extra=None):
        headers = {}
        data = None
        if body is not None:
            data = json.dumps(body).encode(); headers["Content-Type"] = "application/json"
        if raw is not None:
            data = raw; headers["Content-Type"] = ctype
        if self.csrf(): headers["X-XSRF-TOKEN"] = self.csrf()
        if extra: headers.update(extra)
        req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
        try:
            r = self.op.open(req)
            payload = r.read()
            return r.status, payload, r.headers
        except urllib.error.HTTPError as e:
            return e.code, e.read(), e.headers
    def j(self, method, path, body=None, **kw):
        s, p, h = self.call(method, path, body, **kw)
        try: return s, json.loads(p or b"null")
        except Exception: return s, p
    def login(self, email, pw):
        s, r = self.j("POST", "/auth/login", {"email": email, "password": pw, "rememberMe": False})
        assert s == 200, (email, s, r)

def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ((" :: " + str(detail)) if not cond else ""))

def multipart(files):
    boundary = "----e2e" + uuid.uuid4().hex
    out = io.BytesIO()
    for name, ctype, content in files:
        out.write(f"--{boundary}\r\nContent-Disposition: form-data; name=\"files\"; filename=\"{name}\"\r\nContent-Type: {ctype}\r\n\r\n".encode())
        out.write(content); out.write(b"\r\n")
    out.write(f"--{boundary}--\r\n".encode())
    return out.getvalue(), f"multipart/form-data; boundary={boundary}"

import zipfile
def docx():
    b = io.BytesIO()
    with zipfile.ZipFile(b, "w") as z:
        z.writestr("[Content_Types].xml", "<Types/>"); z.writestr("word/document.xml", "<w:document/>")
    return b.getvalue()

PDF = b"%PDF-1.4\n1 0 obj <<>> endobj\ntrailer\n%%EOF\n"
DOCX_T = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
sfx = uuid.uuid4().hex[:6]

admin = Client("admin"); admin.login(ADMIN_EMAIL, ADMIN_PASSWORD)
users = {}
for key, role in [("rec1", "RECEPTIONIST"), ("rec2", "RECEPTIONIST"), ("tech1", "TECHNICIAN"), ("tech2", "TECHNICIAN"), ("admin2", "ADMIN")]:
    s, r = admin.j("POST", "/users", {"email": f"{key}-{sfx}@example.test", "displayName": key, "password": "Passw0rd-Long-1234", "role": role})
    assert s in (200, 201), (key, s, r)
    users[key] = Client(key); users[key].login(f"{key}-{sfx}@example.test", "Passw0rd-Long-1234")
rec1, rec2, tech1, tech2, admin2 = (users[k] for k in ["rec1", "rec2", "tech1", "tech2", "admin2"])

# --- P1: intake -------------------------------------------------------------
receipt_body = lambda phone, **kw: {
    "customerName": "Khách E2E bí mật", "customerPhone": phone, "requestNumber": "REQ-" + sfx, "customerEmail": "kh@example.test",
    "customerAddress": "Cần Thơ", "sampledBy": "Người lấy", "growingArea": "Vùng A", "deliveredBy": "Người giao",
    "sampleName": "Sầu riêng", "quantity": "2 kg", "packaging": "Túi", "sealStatus": "INTACT", "sampleCondition": "Nguyên vẹn",
    "requestedParameters": "Cadimi", "expectedDate": "2030-01-10", "acceptanceDecision": "ACCEPTED",
    "samples": [{"sourceCode": "CT001", "productType": "Sầu riêng", "name": "Mẫu 1", "requestedParameters": "Cadimi"},
                {"sourceCode": "CT002", "productType": "Sầu riêng", "name": "Mẫu 2", "requestedParameters": "Cadimi"}], **kw}
s, receipt = rec1.j("POST", "/receipts", receipt_body("0912345678"))
check("P1 receipt created (201) with 2 samples", s == 201 and len(receipt["samples"]) == 2, (s, receipt))
codes = [x["sampleCode"] for x in receipt["samples"]]
check("P2 sample codes distinct + start PENDING_CODING", len(set(codes)) == 2 and all(x["codingStatus"] == "PENDING_CODING" for x in receipt["samples"]), codes)
s, bad = rec1.j("POST", "/receipts", {**receipt_body("0900000000"), "sealStatus": None})
check("P1 validation: missing seal status -> 400", s == 400, s)
check("P1 technician cannot read full receipt API (403)", tech1.call("GET", f"/receipts/{receipt['id']}")[0] == 403)
check("P1 anonymous gets 401", Client("anon").call("GET", f"/receipts/{receipt['id']}")[0] == 401)
s, page = rec1.j("GET", "/receipts?page=0&size=1&sort=receivedAt,desc")
check("P1 pagination contract", s == 200 and page["size"] == 1, page if s != 200 else "")
check("P1 invalid sort -> 400", rec1.call("GET", "/receipts?sort=passwordHash,asc")[0] == 400)
s, p, h = rec1.call("GET", f"/receipts/{receipt['id']}/form-3")
check("P1 Form 3 export returns DOCX zip (Word not verified)", s == 200 and p[:2] == b"PK", s)

# --- P2: coding gate --------------------------------------------------------
sid1, sid2 = receipt["samples"][0]["id"], receipt["samples"][1]["id"]
transfer_req = lambda ids: {"receiptId": receipt["id"], "receivingDepartment": "KCS", "transferDate": "2030-01-02", "resultsDueDate": "2030-01-09",
    "returnToCustomer": False, "items": [{"sampleId": i, "testMethod": "PP"} for i in ids]}
s, r = rec1.j("POST", "/transfers", transfer_req([sid1]))
check("P2 transfer blocked for unverified sample (409)", s == 409, (s, r))
s, r = rec1.j("GET", "/receipts?transferable=true&size=100")
check("P2 unverified receipt not offered as transferable", all(x["id"] != receipt["id"] for x in r["content"]), "")
check("P2 technician cannot use coding API (403)", tech1.call("POST", f"/samples/{sid1}/coding/submit")[0] == 403)
check("P2 verify before submit -> 409", rec2.call("POST", f"/samples/{sid1}/coding/verify")[0] == 409)
s, r = rec1.j("POST", f"/samples/{sid1}/coding/submit"); check("P2 coder submits", s == 200 and r["codingStatus"] == "PENDING_VERIFICATION", r)
check("P2 coder cannot self-verify (403)", rec1.call("POST", f"/samples/{sid1}/coding/verify")[0] == 403)
s, r = rec2.j("POST", f"/samples/{sid1}/coding/verify"); check("P2 second person verifies", s == 200 and r["codingStatus"] == "VERIFIED", r)
rec1.j("POST", f"/samples/{sid2}/coding/submit")
s, r = rec2.j("POST", f"/samples/{sid2}/coding/reject", {"reason": "Sai nhãn"}); check("P2 reject -> CORRECTION_REQUIRED", s == 200 and r["codingStatus"] == "CORRECTION_REQUIRED", r)
s, r = rec1.j("POST", f"/samples/{sid2}/coding/correct", {"sourceCode": "CT003", "reason": "Sửa mã nguồn"})
check("P2 correction issues new code", s == 200 and r["sampleCode"].startswith("CT003-") and r["sampleCode"] != codes[1], r)
s, ev = rec1.j("GET", f"/samples/{sid2}/coding/events")
check("P2 history keeps old code", [e["eventType"] for e in ev] == ["SUBMITTED", "REJECTED", "CORRECTED"] and ev[-1]["oldCode"] == codes[1], ev)

# --- P1: transfer & receiving ----------------------------------------------
s, tr = rec1.j("POST", "/transfers", transfer_req([sid1])); check("P1 transfer draft for verified sample", s in (200, 201), (s, tr))
check("P1 technician cannot see draft transfer", tech1.call("GET", f"/receiving/transfers/{tr['id']}")[0] == 404)
s, r = rec1.j("POST", f"/transfers/{tr['id']}/submit"); check("P1 submit transfer", s == 200 and r["status"] == "PENDING", r)
s, r = tech1.j("POST", f"/transfers/{tr['id']}/decision", {"decision": "REJECTED"}); check("P1 reject without reason -> 400", s == 400, r)
s, r = tech1.j("POST", f"/transfers/{tr['id']}/decision", {"decision": "ACCEPTED", "receivingNote": "Đủ", "labRetain": False}); check("P1 KCS accepts", s == 200 and r["status"] == "ACCEPTED", r)
s, r = tech2.j("POST", f"/transfers/{tr['id']}/decision", {"decision": "ACCEPTED", "labRetain": False}); check("P1 second decision -> 409", s == 409, (s, r))
item_id = r_item = None
s, tr_full = tech1.j("GET", f"/receiving/transfers/{tr['id']}")
item_id = tr_full["items"][0]["id"]
check("P1 KCS projection hides customer name", "Khách E2E" not in json.dumps(tr_full, ensure_ascii=False), "")

# --- P1: testing record (Phiếu 5) -------------------------------------------
rec_req = {"transferItemId": item_id, "analysisStart": "2030-01-03T01:00:00Z", "analysisEnd": "2030-01-03T03:00:00Z", "recoveryPercent": 98.5,
           "equipment": [], "reagents": [], "measurements": [{"type": "RAW", "replicate": "1", "result": "0.1"}]}
s, trec = tech1.j("POST", "/testing-records", rec_req); check("P1 Phiếu 5 created", s in (200, 201), (s, trec))
check("P1 submit Phiếu 5", tech1.call("POST", f"/testing-records/{trec['id']}/submit")[0] == 200)
check("P1 technician cannot review (403)", tech1.call("POST", f"/testing-records/{trec['id']}/review")[0] == 403)
s, r = admin.j("POST", f"/testing-records/{trec['id']}/review", {"decision": "REVIEWED", "reviewNote": "Đạt"}); check("P1 admin reviews", s == 200 and r["status"] == "REVIEWED", r)

# --- P3: result dossier ------------------------------------------------------
s, rel = tech1.j("POST", "/result-releases", {"testingRecordId": trec["id"]}); check("P3 draft created", s == 201, (s, rel))
rid = rel["id"]
body, ct = multipart([("kq.pdf", "application/pdf", PDF), ("bang.docx", DOCX_T, docx())])
s, r = tech2.j("POST", f"/result-releases/{rid}/files", raw=body, ctype=ct); check("P3 other technician cannot upload (403)", s == 403, (s, r))
body, ct = multipart([("fake.pdf", "application/pdf", b"MZ not pdf")])
s, r = tech1.j("POST", f"/result-releases/{rid}/files", raw=body, ctype=ct); check("P3 spoofed PDF rejected (415)", s == 415, (s, r))
body, ct = multipart([("big.pdf", "application/pdf", b"%PDF-" + b"a" * (10 * 1024 * 1024 + 10) + b"\n%%EOF")])
s, r = tech1.j("POST", f"/result-releases/{rid}/files", raw=body, ctype=ct); check("P3 >10MB rejected (413)", s == 413, (s, r if s != 413 else ""))
body, ct = multipart([("kq.pdf", "application/pdf", PDF), ("bang.docx", DOCX_T, docx())])
s, r = tech1.j("POST", f"/result-releases/{rid}/files", raw=body, ctype=ct); check("P3 PDF+DOCX uploaded", s == 200 and len(r["files"]) == 2, (s, r))
s, r = admin.j("POST", f"/result-releases/{rid}/publish"); check("P3 publish before submit -> 409", s == 409, (s, r))
s, r = tech1.j("POST", f"/result-releases/{rid}/submit"); check("P3 submit without report rows -> 409", s == 409, (s, r))
s, r = tech2.j("PUT", f"/result-releases/{rid}/report", {"rows": [{"parameter": "Cd", "result": "KPH"}]}); check("P3 other technician cannot edit report (403)", s == 403, (s, r))
s, r = tech1.j("PUT", f"/result-releases/{rid}/report", {"rows": [], "qualityHeadName": "A"}); check("P3 empty report rows -> 400", s == 400, (s, r))
s, r = tech1.j("PUT", f"/result-releases/{rid}/report", {"rows": [{"parameter": "Auramine O (Phần vỏ)", "unit": "µg/kg", "method": "CFC-01:2026 (1)(2)(3)", "result": "KPH", "note": "LOQ = 1,5 µg/kg"}, {"parameter": "Cadimium (Cd)", "unit": "µg/kg", "method": "CFC-02:2026 (1)(2)(3)", "result": "< LOQ", "note": "LOQ = 30 µg/kg"}], "qualityHeadName": "Phan Trường Tiền", "directorName": "Nguyễn Công Huân"})
check("P3 report rows saved", s == 200 and len(r["items"]) == 2, (s, r))
s, p, h = tech1.call("GET", f"/result-releases/{rid}/report-preview"); check("P3 report preview is a PDF", s == 200 and p[:5] == b"%PDF-" and h.get("Content-Type") == "application/pdf", (s, p[:20]))
check("P3 receptionist cannot preview (403)", rec1.call("GET", f"/result-releases/{rid}/report-preview")[0] == 403)
check("P3 submit for review", tech1.call("POST", f"/result-releases/{rid}/submit")[0] == 200)
check("P3 technician cannot publish (403)", tech1.call("POST", f"/result-releases/{rid}/publish")[0] == 403)
s, pub = admin2.j("POST", f"/result-releases/{rid}/publish"); check("P3 admin publishes", s == 200 and pub["status"] == "PUBLISHED" and len(pub["lookupCode"]) == 16, (s, pub))
code = pub["lookupCode"]
import re as _re
check("P3 result number like SR092026-001", _re.fullmatch(r"SR\d{6}-\d{3}", pub["resultNumber"] or ""), pub["resultNumber"])
check("P3 only the generated KQKN PDF is public by default", [f["originalName"] for f in pub["files"] if f["publicDownload"]] == ["KQKN-" + pub["resultNumber"] + ".pdf"], pub["files"])
s, evs = admin.j("GET", f"/result-releases/{rid}/events"); check("P3 history", [e["eventType"] for e in evs] == ["CREATED", "FILE_ADDED", "FILE_ADDED", "REPORT_UPDATED", "SUBMITTED", "PUBLISHED"], evs)

# --- P4: public lookup -------------------------------------------------------
pub_c = Client("customer")
s, r, h = pub_c.call("POST", "/public/results/lookup", {"code": code, "phoneLast4": "5678"})
res = json.loads(r) if s == 200 else r
check("P4 lookup with code + last4 -> 200 (result rows + KQKN PDF)", s == 200 and res["files"][0]["name"].startswith("KQKN-") and len(res["items"]) == 2 and res["items"][1]["result"] == "< LOQ", (s, r))
check("P4 headers no-store + noindex", "no-store" in h.get("Cache-Control", "") and "noindex" in h.get("X-Robots-Tag", ""), dict(h))
check("P4 no PII in response", all(x not in r.decode() for x in ["Khách E2E", "0912345678", "kh@example.test", "Cần Thơ"]), r.decode())
s, r, h = pub_c.call("POST", "/public/results/download", {"code": code, "phoneLast4": "5678", "fileId": res["files"][0]["id"]})
if os.environ.get("E2E_SAVE_PDF") and s == 200: open(os.environ["E2E_SAVE_PDF"], "wb").write(r)
check("P4 download generated KQKN PDF", s == 200 and r.startswith(b"%PDF") and len(r) > 5000 and "attachment" in h.get("Content-Disposition", ""), (s, r[:20]))
docx_id = [f for f in pub["files"] if f["originalName"] == "bang.docx"][0]["id"]
check("P4 internal attachment download blocked (404)", pub_c.call("POST", "/public/results/download", {"code": code, "phoneLast4": "5678", "fileId": docx_id})[0] == 404)
check("P4 wrong phone -> 404 neutral", pub_c.call("POST", "/public/results/lookup", {"code": code, "phoneLast4": "0000"})[0] == 404)
check("P4 internal API closed to anonymous (401)", pub_c.call("GET", f"/result-releases/{rid}")[0] == 401)
check("P4 other public paths denied", pub_c.call("GET", "/public/results/lookup")[0] in (401, 403, 405))
if os.environ.get("KEEP"):
    # leave receipt/release published for UI checks; also a second submitted-awaiting scenario is not needed
    # also leave an editable v2 draft (revoke v1 then revise) so UI checks can exercise the report editor
    admin2.j("POST", f"/result-releases/{rid}/revoke", {"reason": "Tạo bản nháp v2 để thử giao diện"})
    s, v2 = tech1.j("POST", f"/result-releases/{rid}/revisions")
    json.dump({"sfx": sfx, "code": code, "rid": rid, "draftId": v2["id"], "receiptId": receipt["id"], "sampleIds": [sid1, sid2], "trecId": trec["id"]}, open(os.environ.get("E2E_STATE", "e2e-state.json"), "w"))
    print("KEEP: state saved"); sys.exit(0)
s, r = admin2.j("POST", f"/result-releases/{rid}/revoke", {"reason": "Kiểm thử thu hồi"}); check("P3 revoke", s == 200 and r["status"] == "REVOKED", (s, r))
check("P4 revoked -> lookup 404", Client("c2").call("POST", "/public/results/lookup", {"code": code, "phoneLast4": "5678"})[0] == 404)
s, v2 = tech1.j("POST", f"/result-releases/{rid}/revisions"); check("P3 new version v2 draft", s == 201 and v2["version"] == 2 and v2["status"] == "DRAFT", (s, v2))
# brute force
bf = Client("attacker"); codes_seen = []
for i in range(6): codes_seen.append(bf.call("POST", "/public/results/lookup", {"code": code, "phoneLast4": f"{i:04d}"})[0])
first429 = codes_seen.index(429) if 429 in codes_seen else -1
check("P4 brute force throttled: 5 failed tries per code in total (2 already used above) then 429", codes_seen[:3] == [404] * 3 and first429 == 3 and set(codes_seen[3:]) == {429}, codes_seen)

# --- Danh sách: sort/tìm/lọc ngày do backend xử lý ---------------------------------------
s, r = admin.j("GET", "/transfers?sort=transferCode,asc&size=5"); check("LIST transfers paged + sorted by server", s == 200 and r["sort"] == "transferCode,asc" and "totalElements" in r, (s, r))
codes_sorted = [x["transferCode"] for x in r["content"]]; check("LIST transfers order is ascending", codes_sorted == sorted(codes_sorted), codes_sorted)
s, r = admin.j("GET", "/transfers?sort=transferCode,desc&size=5"); d = [x["transferCode"] for x in r["content"]]; check("LIST transfers descending", d == sorted(d, reverse=True), d)
check("LIST transfers invalid sort -> 400", admin.call("GET", "/transfers?sort=passwordHash,asc")[0] == 400)
check("LIST transfers size>100 -> 400", admin.call("GET", "/transfers?size=101")[0] == 400)
check("LIST transfers technician cannot sort by customer -> 400", tech1.call("GET", "/transfers?sort=customerName,asc")[0] == 400)
s, r = admin.j("GET", "/transfers?q=" + urllib.parse.quote("Khách E2E")); check("LIST transfers search by customer (admin)", s == 200 and r["totalElements"] >= 1, (s, r))
s, r = tech1.j("GET", "/transfers?q=" + urllib.parse.quote("Khách E2E")); check("LIST transfers technician cannot search customer", s == 200 and r["totalElements"] == 0, (s, r))
s, r = admin.j("GET", "/transfers?from=2099-01-01"); check("LIST transfers date filter", s == 200 and r["totalElements"] == 0, r)
check("LIST transfers bad date range -> 400", admin.call("GET", "/transfers?from=2030-02-01&to=2030-01-01")[0] == 400)
s, r = admin.j("GET", "/testing-records?q=" + trec["recordCode"] + "&sort=recordCode,asc"); check("LIST testing records search + sort", s == 200 and r["totalElements"] == 1, (s, r))
s, r = tech1.j("GET", "/testing-records/candidates"); check("LIST testing candidates endpoint", s == 200 and isinstance(r, list), (s, r))
s, r = admin.j("GET", "/result-releases?sort=updatedAt,desc&q=" + trec["recordCode"]); check("LIST result releases search + sort", s == 200 and r["totalElements"] >= 1, (s, r))
s, r = tech1.j("GET", "/result-releases/ready"); check("LIST ready records endpoint", s == 200 and isinstance(r, list), (s, r))
s, r = tech1.j("GET", "/notifications?q=" + urllib.parse.quote("mẫu") + "&sort=title,asc"); check("LIST notifications search + sort", s == 200 and "content" in r, (s, r))
check("LIST notifications invalid sort -> 400", tech1.call("GET", "/notifications?sort=message,asc")[0] == 400)
s, r = admin.j("GET", "/users?role=TECHNICIAN&sort=displayName,asc&q=tech"); check("LIST users search/filter/sort", s == 200 and r["totalElements"] >= 2 and all(u["role"] == "TECHNICIAN" for u in r["content"]), (s, r))
check("LIST users forbidden for technician", tech1.call("GET", "/users")[0] == 403)

failed = [r for r in results if not r[1]]
print(f"\n{len(results) - len(failed)}/{len(results)} checks passed")
sys.exit(1 if failed else 0)
