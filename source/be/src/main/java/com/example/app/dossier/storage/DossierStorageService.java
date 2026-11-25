package com.example.app.dossier.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class DossierStorageService {
    private final JdbcTemplate jdbc;
    private final Path receipts, results;
    private final List<Path> allowed, forbidden;
    private final Path exports;
    private final long reserve, warnBytes;
    private final int warnPercent;
    public DossierStorageService(JdbcTemplate jdbc,
            @Value("${app.storage.root:storage/receipts}") String receipts,
            @Value("${app.storage.results-root:storage/results}") String results,
            @Value("${app.storage.allowed-roots:storage}") String allowed,
            @Value("${app.storage.reserve-bytes:67108864}") long reserve,
            @Value("${app.storage.warn-bytes:5368709120}") long warnBytes,
            @Value("${app.storage.warn-percent:10}") int warnPercent,
            @Value("${app.dossier-exports.root:storage/exports}") String exports,
            @Value("${app.storage.avatar-root:storage/avatars}") String avatars) {
        this.jdbc = jdbc; this.receipts = absolute(receipts); this.results = absolute(results);
        this.allowed = Arrays.stream(allowed.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(DossierStorageService::absolute).toList();
        this.exports = absolute(exports);
        this.forbidden = List.of(this.exports, absolute(avatars));
        this.reserve = Math.max(0, reserve); this.warnBytes = Math.max(0, warnBytes);
        this.warnPercent = Math.max(0, Math.min(100, warnPercent));
    }
    private static Path absolute(String path) { return Path.of(path).toAbsolutePath().normalize(); }
    public record Location(long id, String label, String legacyKey, String rootPath, String marker, String volumeIdentity, Instant validatedAt) {}
    public record Health(Location location, String status, long freeBytes, long totalBytes, boolean warning, long files, long bytes) {}
    /** Server areas that uploads and exports also depend on, but that changing the upload target does not free. */
    public record Area(String label, String status, long freeBytes, long totalBytes, boolean warning) {}
    public record Settings(Long locationId, long version) {}
    public record Overview(Settings settings, List<Health> locations, List<Area> areas, List<String> allowedRoots) {}
    public record Written(long locationId, String relativePath, Path path, long size, String checksum) {}
    public Settings settings() {
        return jdbc.queryForObject("SELECT location_id,version FROM storage_write_settings WHERE id=1",
                (r,n) -> new Settings(r.getObject(1, Long.class), r.getLong(2)));
    }
    public Overview overview() {
        Map<Long, long[]> usage = new HashMap<>();
        jdbc.query("""
            SELECT storage_location_id,COUNT(*),COALESCE(SUM(size),0) FROM (
              SELECT storage_location_id,size FROM receipt_attachments
              UNION ALL SELECT storage_location_id,size FROM result_release_files) f GROUP BY storage_location_id
            """, row -> { usage.put(row.getLong(1), new long[]{row.getLong(2), row.getLong(3)}); });
        return new Overview(settings(),
                locations().stream().map(l -> health(l, usage.getOrDefault(l.id, new long[2]))).toList(),
                List.of(area("Vùng tạm tải lên / tạo PDF", Path.of(System.getProperty("java.io.tmpdir"))), area("Vùng gói xuất tạm", exports)),
                allowed.stream().map(Path::toString).toList());
    }
    private static Location location(java.sql.ResultSet r) throws java.sql.SQLException {
        Timestamp validated = r.getTimestamp("validated_at");
        return new Location(r.getLong("id"), r.getString("label"), r.getString("legacy_key"), r.getString("root_path"),
                r.getString("marker"), r.getString("volume_identity"), validated == null ? null : validated.toInstant());
    }
    private List<Location> locations() {
        return jdbc.query("SELECT * FROM storage_locations ORDER BY id", (r,n) -> location(r));
    }
    private Location location(long id) {
        return jdbc.query("SELECT * FROM storage_locations WHERE id=?", (r,n) -> location(r), id)
                .stream().findFirst().orElseThrow(() -> error(404,"Không tìm thấy nơi lưu trữ"));
    }
    private Path root(Location l) { return l.legacyKey == null ? absolute(l.rootPath) : "RECEIPTS".equals(l.legacyKey) ? receipts : results; }
    private boolean low(long free, long total) {
        return free < Math.max(reserve, warnBytes) || (total > 0 && (double) free / total * 100 < warnPercent);
    }
    private Health health(Location l, long[] used) {
        try {
            Path p = root(l);
            if (l.legacyKey != null && used[0] == 0 && !Files.exists(p)) return new Health(l,"EMPTY",0,0,false,0,0);
            checkRoot(l, p);
            var fs = Files.getFileStore(p); long free = fs.getUsableSpace(), total = fs.getTotalSpace();
            boolean moved = volumeChanged(l, p);
            return new Health(l, moved ? "VOLUME_CHANGED" : "ONLINE", free, total, moved || low(free,total), used[0], used[1]);
        } catch (IOException | RuntimeException e) { return new Health(l,"OFFLINE",0,0,true,used[0],used[1]); }
    }
    private Area area(String label, Path path) {
        try {
            Path p = path; while (p != null && !Files.isDirectory(p)) p = p.getParent();
            if (p == null) throw new IOException("Unavailable");
            var fs = Files.getFileStore(p); long free = fs.getUsableSpace(), total = fs.getTotalSpace();
            return new Area(label,"ONLINE",free,total,low(free,total));
        } catch (IOException | RuntimeException e) { return new Area(label,"OFFLINE",0,0,true); }
    }
    private static String volume(Path p) throws IOException {
        var fs = Files.getFileStore(p); return fs.name() + "|" + fs.type();
    }
    /**
     * The marker file proves this is the registered disk: an unmounted disk leaves an empty directory without it.
     * Device names (sda/sdb) may change after a reboot, so a changed volume never blocks reading; it blocks new
     * uploads until an Admin validates the location again.
     */
    private void checkRoot(Location l, Path p) throws IOException {
        if (!Files.isDirectory(p)) throw new IOException("Storage unavailable");
        if (l.legacyKey != null) return;
        if (!p.equals(p.toRealPath()) || l.marker == null
                || !Files.readString(p.resolve(".cfc-storage-id")).trim().equals(l.marker)) throw new IOException("Storage identity mismatch");
    }
    private boolean volumeChanged(Location l, Path p) throws IOException {
        return l.legacyKey == null && !volume(p).equals(l.volumeIdentity);
    }
    private Path safe(Path root, String relative, boolean creating) throws IOException {
        if (relative == null || relative.isBlank()) throw new IOException("Invalid path");
        Path child;
        try { child = Path.of(relative); } catch (InvalidPathException e) { throw new IOException("Invalid path"); }
        if (child.isAbsolute() || child.normalize().startsWith("..") || java.util.stream.StreamSupport.stream(child.spliterator(),false).anyMatch(x -> x.toString().equals(".."))) throw new IOException("Invalid path");
        Path p = root.resolve(child).normalize();
        if (!p.startsWith(root) || p.equals(root)) throw new IOException("Invalid path");
        Path current = root;
        for (Path part : root.relativize(p)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic links are not storage files");
        }
        if (!creating && !Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException("File unavailable");
        return p;
    }
    /** Resolves a stored file on the disk it was written to, never on the disk that currently receives uploads. */
    public Path read(long locationId, String relative, String legacyName) {
        Location l = location(locationId); Path p = root(l);
        try { checkRoot(l,p); }
        catch (IOException e) { throw error(503,"Ổ lưu trữ chưa truy cập được; cần Admin kiểm tra kết nối"); }
        try {
            // Names written before V24 may contain ':' (phone photos); only new relative paths are strict.
            if (relative != null && (relative.contains("\\") || relative.contains(":"))) throw new IOException("Invalid path");
            return safe(p, relative == null ? legacyName : relative, false);
        } catch (NoSuchFileException e) { throw error(404,"File không còn trên vùng lưu trữ; cần Admin đối soát"); }
        catch (IOException e) { throw error(503,"Không đọc được file trên ổ lưu trữ; cần Admin kiểm tra"); }
    }
    @Transactional
    public Overview register(String label, String path, long actor) {
        Path p;
        try {
            try { p = absolute(path.trim()); } catch (InvalidPathException e) { throw error(400,"Đường dẫn trên máy chủ không hợp lệ"); }
            if (!p.equals(p.toRealPath()) || !Files.isDirectory(p)) throw new IOException("Not a directory");
            boolean permitted = false;
            for (Path a : allowed) if (Files.isDirectory(a) && p.startsWith(a.toRealPath())) permitted = true;
            if (!permitted) throw error(400,"Đường dẫn nằm ngoài các vùng lưu trữ được máy chủ cho phép");
            for (Path f : forbidden) if (p.startsWith(f) || f.startsWith(p)) throw error(400,"Vùng này dành cho gói xuất tạm hoặc ảnh đại diện; chọn thư mục riêng cho hồ sơ");
            for (Location l : locations()) {
                Path existing = root(l);
                if (p.startsWith(existing) || existing.startsWith(p)) throw error(409,"Vị trí bị trùng hoặc lồng với nơi lưu đã đăng ký");
            }
            // A fresh mounted directory is supplied by the operator, never created as a fallback for a missing mount.
            Path marker = p.resolve(".cfc-storage-id");
            if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) throw error(409,"Thư mục đã có định danh lưu trữ; cần đối soát trước khi đăng ký lại");
            String token = UUID.randomUUID().toString();
            Files.writeString(marker, token, StandardOpenOption.CREATE_NEW);
            rollbackDelete(marker);
            String identity = volume(p);
            jdbc.update("INSERT INTO storage_locations(label,root_path,marker,volume_identity) VALUES (?,?,?,?)",label.trim(),p.toString(),token,identity);
            Long id = jdbc.queryForObject("SELECT id FROM storage_locations WHERE root_path=?",Long.class,p.toString());
            validate(id, actor);
            audit(actor,"STORAGE_REGISTER",id.toString(),null);
            return overview();
        } catch (IOException e) { throw error(400,"Không thể đăng ký nơi lưu; kiểm tra thư mục, mount và quyền ghi"); }
    }
    @Transactional
    public Overview validate(long id, long actor) {
        Location l = location(id);
        if (l.legacyKey != null) throw error(400,"Vị trí cũ chỉ dùng để đọc và tương thích; hãy đăng ký thư mục mới để chuyển upload");
        Path test = null;
        try {
            Path p = root(l); checkRoot(l,p);
            if (Files.getFileStore(p).getUsableSpace() < reserve + 4096) throw error(507,"Nơi lưu không đủ dung lượng dự phòng");
            test = Files.createTempFile(p,".cfc-check-",".tmp");
            Files.writeString(test,"cfc-storage-check");
            if (!Files.readString(test).equals("cfc-storage-check")) throw new IOException("Probe failed");
            Files.delete(test); test = null;
            jdbc.update("UPDATE storage_locations SET validated_at=?,volume_identity=? WHERE id=?",now(),volume(p),id);
            audit(actor,"STORAGE_VALIDATE",Long.toString(id),null);
            return overview();
        } catch (IOException e) { throw error(503,"Kiểm tra ổ lưu thất bại; kiểm tra kết nối và quyền đọc/ghi"); }
        finally { if (test != null) try { Files.deleteIfExists(test); } catch (IOException ignored) {} }
    }
    @Transactional
    public Overview activate(long id, long version, long actor) {
        validate(id,actor);
        Settings before = settings();
        if (jdbc.update("UPDATE storage_write_settings SET location_id=?,version=version+1 WHERE id=1 AND version=?", id, version) != 1)
            throw error(409,"Cấu hình vừa được Admin khác thay đổi. Làm mới trang rồi thử lại");
        audit(actor,"STORAGE_ACTIVATE",Long.toString(id),"from=" + before.locationId + ";version=" + version);
        return overview();
    }
    @Transactional
    public String folder(long receiptId) {
        var row = jdbc.queryForMap("SELECT archive_path,request_number,request_number_unique,received_at FROM receipts WHERE id=? FOR UPDATE",receiptId);
        String folder = (String) row.get("archive_path");
        if (folder == null) {
            folder = ArchivePaths.folder(receiptId,(String)row.get("request_number"),
                    ArchivePaths.owns((String)row.get("request_number"),(String)row.get("request_number_unique")),((java.sql.Timestamp)row.get("received_at")).toInstant());
            jdbc.update("UPDATE receipts SET archive_path=? WHERE id=?",folder,receiptId);
        }
        return folder;
    }
    public Written write(long legacyId, long receiptId, String section, String fileName, InputStream input, long expectedSize) {
        Long selected = settings().locationId;
        // The location is fixed here for the whole upload; a later switch only affects the next upload.
        Location l = location(selected == null ? legacyId : selected); Path p = root(l), temp = null;
        try (input) {
            try {
                if (l.legacyKey != null) Files.createDirectories(p);
                checkRoot(l,p);
                if (volumeChanged(l,p)) throw new IOException("Volume changed");
            } catch (IOException e) { throw error(503,"Ổ nhận upload chưa sẵn sàng; file chưa được lưu. Cần Admin kiểm tra ổ hoặc chọn nơi lưu khác"); }
            if (expectedSize < 0 || expectedSize > 105L*1024*1024) throw error(400,"Kích thước file không hợp lệ");
            if (Files.getFileStore(p).getUsableSpace() < reserve + expectedSize) throw error(507,"Ổ lưu gần đầy, file chưa được lưu. Cần Admin chọn nơi lưu upload mới");
            String relative = l.legacyKey == null ? folder(receiptId) + "/" + section + "/" + fileName : fileName;
            Path destination = safe(p,relative,true);
            Files.createDirectories(destination.getParent());
            safe(p,relative,true);
            temp = Files.createTempFile(destination.getParent(),".upload-",".partial");
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); long size = 0;
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE); OutputStream out = Channels.newOutputStream(channel)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) {
                    size += count; if (size > expectedSize) throw new IOException("Unexpected file size");
                    if (Files.getFileStore(p).getUsableSpace() < reserve + count) throw error(507,"Ổ lưu không còn đủ dung lượng; file chưa được lưu");
                    digest.update(buffer,0,count); out.write(buffer,0,count);
                }
                channel.force(true);
            }
            if (size != expectedSize) throw new IOException("Incomplete file");
            Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE); temp = null;
            rollbackDelete(destination);
            return new Written(l.id,relative,destination,size,HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) { throw error(507,"Không lưu được file. Kiểm tra dung lượng, kết nối và quyền ghi ổ lưu trữ"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        finally { if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) {} }
    }
    private void rollbackDelete(Path path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            }
        });
    }
    public void audit(long actor, String action, String id, String detail) {
        jdbc.update("INSERT INTO dossier_audit(actor_user_id,action,object_id,detail,created_at) VALUES (?,?,?,?,?)",actor,action,id,detail,now());
    }
    /** Application clock through the driver: the MySQL session time zone may differ from the JDBC time zone (Windows). */
    public static Timestamp now() { return Timestamp.from(Instant.now()); }
    private static ResponseStatusException error(int status, String message) { return new ResponseStatusException(HttpStatus.valueOf(status),message); }
}
