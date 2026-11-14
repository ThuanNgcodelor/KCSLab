package com.example.app.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;

@Service
public class AvatarStorageService {
    static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    static final long MAX_PIXELS = 40_000_000L;
    static final int OUTPUT_SIZE = 320;
    private final Path root;

    public AvatarStorageService(@Value("${app.storage.avatar-root:storage/avatars}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredAvatar store(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn ảnh đại diện");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện tối đa 5 MB");
        }

        Path temporary = null;
        try {
            byte[] input = file.getBytes();
            BufferedImage source = decodeSupportedImage(input);
            BufferedImage avatar = cropAndResize(source);
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, "avatar-", ".jpg");
            writeJpeg(avatar, temporary);
            Path destination = avatarPath(userId);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredAvatar(Files.size(destination), OUTPUT_SIZE, OUTPUT_SIZE);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể xử lý ảnh đại diện", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    public AvatarContent load(Long userId) {
        Path path = avatarPath(userId);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có ảnh đại diện");
        }
        try {
            return new AvatarContent(new FileSystemResource(path), Files.size(path), Files.getLastModifiedTime(path).toMillis());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể đọc ảnh đại diện", exception);
        }
    }

    public void delete(Long userId) {
        try {
            Files.deleteIfExists(avatarPath(userId));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể xóa ảnh đại diện", exception);
        }
    }

    private BufferedImage decodeSupportedImage(byte[] input) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (stream == null) throw unsupportedImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw unsupportedImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw unsupportedImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 32 || height < 32) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh phải có kích thước tối thiểu 32×32 px");
                }
                if ((long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh có độ phân giải quá lớn");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage cropAndResize(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage result = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, x, y, x + side, y + side, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void writeJpeg(BufferedImage image, Path path) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.82f);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private Path avatarPath(Long userId) {
        return root.resolve("user-" + userId + ".jpg");
    }

    private ResponseStatusException unsupportedImage() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Chỉ hỗ trợ ảnh JPG hoặc PNG");
    }

    public record StoredAvatar(long bytes, int width, int height) { }
    public record AvatarContent(Resource resource, long bytes, long lastModified) { }
}
