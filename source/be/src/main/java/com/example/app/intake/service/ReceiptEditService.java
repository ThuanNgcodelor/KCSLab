package com.example.app.intake.service;

import com.example.app.intake.dto.ReceiptEditResponse;
import com.example.app.intake.dto.UpdateReceiptRequest;
import com.example.app.intake.dto.UpdateReceiptRequest.SampleEdit;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptEditLog;
import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.ReceiptEditLogRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.transfer.repository.SampleTransferItemRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Corrects a saved receipt (Phiếu 3). Receptionists may edit any receipt (the desk shares its work) until a sample is sent to KCS;
 * after that only Admin may edit, with a reason, and the change flows to the Phiếu 4 snapshot. Sample codes, the
 * Số/No., the customer and attachments are never changed here. Every edit is written to {@code receipt_edit_logs}.
 */
@Service
public class ReceiptEditService {
    private final ReceiptRepository receipts;
    private final SampleTransferItemRepository transferItems;
    private final ReceiptEditLogRepository logs;

    public ReceiptEditService(ReceiptRepository receipts, SampleTransferItemRepository transferItems,
                              ReceiptEditLogRepository logs) {
        this.receipts = receipts; this.transferItems = transferItems; this.logs = logs;
    }

    /** True once any sample left the receiving desk (sent to KCS or beyond). */
    public static boolean isLocked(Receipt receipt) {
        return receipt.getSamples().stream().anyMatch(sample -> sample.getStatus() != ReceiptStatus.RECEIVED);
    }

    @Transactional
    public void update(Long id, UpdateReceiptRequest request, User actor) {
        Receipt receipt = receipts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận"));
        boolean locked = isLocked(receipt);
        if (actor.getRole() != Role.ADMIN) {
            if (actor.getRole() != Role.RECEPTIONIST) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ nhân viên tiếp nhận hoặc Admin được sửa phiếu này");
            }
            if (locked) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Hồ sơ đã gửi cho KCS nên không sửa được. Nhờ Admin sửa (có ghi lý do).");
            }
        }
        String reason = blankToNull(request.reason());
        if (locked && reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ đã gửi cho KCS: cần ghi lý do chỉnh sửa");
        }

        Map<Long, Sample> byId = new HashMap<>();
        receipt.getSamples().forEach(sample -> byId.put(sample.getId(), sample));
        Set<Long> seen = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (SampleEdit edit : request.samples()) {
            if (edit.id() == null || !byId.containsKey(edit.id()) || !seen.add(edit.id())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách mẫu không khớp với hồ sơ");
            }
            if (!names.add(edit.name().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tên mẫu \"" + edit.name().trim() + "\" bị trùng. Mỗi mẫu cần một tên riêng.");
            }
        }
        if (seen.size() != byId.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách mẫu không khớp với hồ sơ");
        }

        List<String> changes = new ArrayList<>();
        diff(changes, "Người giao", receipt.getDeliveredBy(), request.deliveredBy().trim());
        diff(changes, "Người lấy mẫu", receipt.getSampledBy(), blankToNull(request.sampledBy()));
        diff(changes, "Mã vùng trồng", receipt.getGrowingArea(), blankToNull(request.growingArea()));
        diff(changes, "Cơ sở đóng gói", receipt.getPackingFacility(), blankToNull(request.packingFacility()));
        diff(changes, "Biển số xe", receipt.getVehicleNumber(), blankToNull(request.vehicleNumber()));
        diff(changes, "Số moóc", receipt.getTrailerNumber(), blankToNull(request.trailerNumber()));
        diff(changes, "Số container", receipt.getContainerNumber(), blankToNull(request.containerNumber()));
        diff(changes, "Số niêm phong", receipt.getSealNumber(), blankToNull(request.sealNumber()));
        diff(changes, "Số seal container", receipt.getContainerSealNumber(), blankToNull(request.containerSealNumber()));
        diff(changes, "Mô tả tình trạng", receipt.getConditionDetail(), blankToNull(request.conditionDetail()));
        diff(changes, "Vận chuyển", receipt.getTransportInfo(), blankToNull(request.transportInfo()));
        diff(changes, "Nhiệt độ khi nhận", receipt.getReceivedTemperature(), request.receivedTemperature());
        diff(changes, "Điều kiện bảo quản", receipt.getStorageCondition(), blankToNull(request.storageCondition()));
        diff(changes, "Bảo quản sau nhận", receipt.getStorageAfterReceipt(), blankToNull(request.storageAfterReceipt()));
        diff(changes, "Hạn dự kiến", receipt.getExpectedDate(), request.expectedDate());
        diff(changes, "Yêu cầu đặc biệt", receipt.getSpecialRequest(), blankToNull(request.specialRequest()));
        if (request.sealStatus() != null) diff(changes, "Niêm phong", receipt.getSealStatus(), request.sealStatus());

        receipt.updateDetails(request.deliveredBy().trim(), blankToNull(request.sampledBy()), request.sampledAt(),
                blankToNull(request.growingArea()), blankToNull(request.packingFacility()),
                blankToNull(request.vehicleNumber()), blankToNull(request.trailerNumber()),
                blankToNull(request.containerNumber()), blankToNull(request.sealNumber()),
                blankToNull(request.containerSealNumber()), request.sealStatus(), blankToNull(request.conditionDetail()),
                blankToNull(request.transportInfo()), request.receivedTemperature(), blankToNull(request.storageCondition()),
                blankToNull(request.storageAfterReceipt()), request.expectedDate(), blankToNull(request.specialRequest()));

        for (SampleEdit edit : request.samples()) {
            Sample sample = byId.get(edit.id());
            String label = "Mẫu " + sample.getSampleCode();
            String oldQuantity = sample.getQuantity(), oldWeight = sample.getWeight();
            String oldCondition = sample.getCondition(), oldParameters = sample.getRequestedParameters();
            String name = edit.name().trim();
            diff(changes, label + " · tên", sample.getName(), name);
            diff(changes, label + " · số lượng", oldQuantity, blankToNull(edit.quantity()));
            diff(changes, label + " · khối lượng", oldWeight, blankToNull(edit.weight()));
            diff(changes, label + " · bao bì", sample.getPackaging(), blankToNull(edit.packaging()));
            diff(changes, label + " · tình trạng", oldCondition, edit.condition().trim());
            diff(changes, label + " · chỉ tiêu", oldParameters, edit.requestedParameters().trim());
            sample.updateDetails(name, blankToNull(edit.quantity()), blankToNull(edit.weight()),
                    blankToNull(edit.packaging()), edit.condition().trim(), edit.requestedParameters().trim());
            transferItems.findBySample_Id(sample.getId())
                    .forEach(item -> item.syncFromSample(sample, oldQuantity, oldWeight, oldCondition, oldParameters));
        }
        if (changes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có thay đổi nào để lưu");
        }
        if (!receipt.getSamples().isEmpty()) receipt.mirrorPrimarySample(receipt.getSamples().get(0));
        String summary = String.join("\n", changes);
        logs.save(new ReceiptEditLog(receipt, actor, reason, locked,
                summary.length() > 8000 ? summary.substring(0, 8000) + "…" : summary));
    }

    @Transactional(readOnly = true)
    public List<ReceiptEditResponse> history(Long receiptId) {
        if (!receipts.existsById(receiptId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận");
        }
        return logs.findByReceipt_IdOrderByIdDesc(receiptId).stream()
                .map(log -> new ReceiptEditResponse(log.getId(), log.getEditedBy().getDisplayName(), log.getEditedAt(),
                        log.getReason(), log.isAfterTransfer(), log.getSummary())).toList();
    }

    private static void diff(List<String> changes, String label, Object before, Object after) {
        String oldValue = before == null ? "" : before.toString();
        String newValue = after == null ? "" : after.toString();
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(label + ": " + (oldValue.isEmpty() ? "(trống)" : oldValue) + " → " + (newValue.isEmpty() ? "(trống)" : newValue));
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
