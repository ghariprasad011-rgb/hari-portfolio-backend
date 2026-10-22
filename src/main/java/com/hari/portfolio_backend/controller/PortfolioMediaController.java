
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.PortfolioMedia;
import com.hari.portfolio_backend.entity.repository.PortfolioMediaRepository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
public class PortfolioMediaController {

    private static final Path UPLOAD_DIR =
            Paths.get("uploads", "portfolio")
                    .toAbsolutePath()
                    .normalize();

    private static final long MAX_FILE_SIZE =
            10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_FILES =
            Map.of(
                    "pdf", "application/pdf",
                    "jpg", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "png", "image/png",
                    "webp", "image/webp",
                    "gif", "image/gif"
            );

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of("CERTIFICATE", "PHOTO", "PROJECT");

    private final PortfolioMediaRepository mediaRepository;

    public PortfolioMediaController(
            PortfolioMediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    // =====================================================
    // 1. ADMIN: LIST ALL MEDIA
    // GET /api/admin/media
    // =====================================================

    @GetMapping
    public List<PortfolioMedia> getAllMedia() {
        return mediaRepository.findAll();
    }

    // =====================================================
    // 2. PUBLIC: LIST APPROVED MEDIA ONLY
    // GET /api/admin/media/public
    // =====================================================

        @GetMapping("/public")
public List<PortfolioMedia> getPublicMedia() {
    System.out.println(
        "========== PUBLIC MEDIA METHOD CALLED =========="
    );

    return mediaRepository.findByPublicVisibleTrue();
}


    // =====================================================
    // 3. ADMIN: UPLOAD MEDIA
    // POST /api/admin/media
    //
    // Multipart fields:
    // file, title, category, publicVisible
    // publicVisible defaults to false.
    // =====================================================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(
                    value = "publicVisible",
                    defaultValue = "false"
            ) boolean publicVisible) {

        if (file == null || file.isEmpty()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Please choose a file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return error(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Maximum file size is 10 MB."
            );
        }

        if (title == null
                || title.isBlank()
                || title.length() > 200) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Enter a title between 1 and 200 characters."
            );
        }

        String normalizedCategory = category == null
                ? ""
                : category.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_CATEGORIES.contains(normalizedCategory)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid category. Use CERTIFICATE, PHOTO or PROJECT."
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file has no filename."
            );
        }

        // Normalize Windows and Unix path separators.
        String normalizedName = originalName.replace("\\", "/");

        String safeOriginalName = normalizedName.substring(
                normalizedName.lastIndexOf('/') + 1
        );

        int dot = safeOriginalName.lastIndexOf('.');

        if (dot < 1 || dot == safeOriginalName.length() - 1) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "The file must have a supported extension."
            );
        }

        String extension = safeOriginalName
                .substring(dot + 1)
                .toLowerCase(Locale.ROOT);

        String expectedContentType = ALLOWED_FILES.get(extension);
        String actualContentType = file.getContentType();

        if (expectedContentType == null
                || actualContentType == null
                || !expectedContentType.equalsIgnoreCase(
                        actualContentType)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file type or mismatched file extension."
            );
        }

        String storedFileName =
                UUID.randomUUID() + "." + extension;

        Path destination = UPLOAD_DIR
                .resolve(storedFileName)
                .normalize();

        if (!destination.startsWith(UPLOAD_DIR)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file path."
            );
        }

        boolean fileWritten = false;

        try {
            Files.createDirectories(UPLOAD_DIR);

            file.transferTo(destination);
            fileWritten = true;

            PortfolioMedia media = new PortfolioMedia();

            media.setTitle(title.trim());
            media.setOriginalFileName(safeOriginalName);
            media.setStoredFileName(storedFileName);
            media.setContentType(expectedContentType);
            media.setCategory(normalizedCategory);

            // Save the requested public visibility setting.
            media.setPublicVisible(publicVisible);

            // Public URL; the file endpoint still checks visibility.
            media.setFileUrl(
                    "/api/admin/media/public/files/"
                            + storedFileName
            );

            PortfolioMedia saved = mediaRepository.save(media);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(saved);

        } catch (IOException | RuntimeException exception) {

            if (fileWritten) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            System.err.println(
                    "Portfolio media upload failed: "
                            + exception.getMessage()
            );

            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upload failed. Check server logs and storage permissions."
            );
        }
    }

    // =====================================================
    // 4. ADMIN: VIEW ANY UPLOADED FILE
    // GET /api/admin/media/files/{filename}
    //
    // SecurityConfig must restrict this route to ADMIN.
    // =====================================================

    @GetMapping("/files/{storedFileName:.+}")
    public ResponseEntity<?> viewAdminMedia(
            @PathVariable String storedFileName) {

        return serveMedia(storedFileName, false);
    }

    // =====================================================
    // 5. PUBLIC: VIEW APPROVED FILES ONLY
    // GET /api/admin/media/public/files/{filename}
    // =====================================================

    @GetMapping("/public/files/{storedFileName:.+}")
    public ResponseEntity<?> viewPublicMedia(
            @PathVariable String storedFileName) {

        return serveMedia(storedFileName, true);
    }

    // =====================================================
    // 6. SHARED FILE-SERVING METHOD
    // =====================================================

    private ResponseEntity<?> serveMedia(
            String storedFileName,
            boolean publicOnly) {

        if (storedFileName == null
                || !storedFileName.matches(
                "[a-fA-F0-9-]{36}\\.(pdf|jpg|jpeg|png|webp|gif)")) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filename."
            );
        }

        // Find the matching database record.
        PortfolioMedia media = mediaRepository.findAll()
                .stream()
                .filter(item -> storedFileName.equals(
                        item.getStoredFileName()))
                .findFirst()
                .orElse(null);

        if (media == null) {
            return error(
                    HttpStatus.NOT_FOUND,
                    "File not found."
            );
        }

        // Never expose files that haven't been approved publicly.
        if (publicOnly && !media.isPublicVisible()) {
            return error(
                    HttpStatus.NOT_FOUND,
                    "File not found."
            );
        }

        Path filePath = UPLOAD_DIR
                .resolve(storedFileName)
                .normalize();

        if (!filePath.startsWith(UPLOAD_DIR)
                || !Files.isRegularFile(filePath)) {

            return error(
                    HttpStatus.NOT_FOUND,
                    "The stored file does not exist."
            );
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return error(
                        HttpStatus.NOT_FOUND,
                        "The file cannot be read."
                );
            }

            MediaType mediaType;

            try {
                mediaType = MediaType.parseMediaType(
                        media.getContentType()
                );
            } catch (Exception exception) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(
                            "X-Content-Type-Options",
                            "nosniff"
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + storedFileName + "\""
                    )
                    .body(resource);

        } catch (IOException exception) {
            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not open the file."
            );
        }
    }

    // =====================================================
    // 7. ADMIN: DELETE FILE AND DATABASE RECORD
    // DELETE /api/admin/media/{id}
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedia(
            @PathVariable Long id) {

        PortfolioMedia media = mediaRepository
                .findById(id)
                .orElse(null);

        if (media == null) {
            return error(
                    HttpStatus.NOT_FOUND,
                    "The requested file was not found."
            );
        }

        String storedName = media.getStoredFileName();

        if (storedName == null
                || storedName.isBlank()
                || !storedName.equals(
                        Paths.get(storedName)
                                .getFileName()
                                .toString())) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "The stored filename is invalid. No file was deleted."
            );
        }

        Path filePath = UPLOAD_DIR
                .resolve(storedName)
                .normalize();

        if (!filePath.startsWith(UPLOAD_DIR)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stored file path."
            );
        }

        try {
            Files.deleteIfExists(filePath);
            mediaRepository.delete(media);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "File and database record deleted successfully."
                    )
            );

        } catch (IOException exception) {
            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete the stored file. "
                            + "The database record was retained."
            );
        }
    }

    // =====================================================
    // 8. STANDARD JSON ERROR RESPONSE
    // =====================================================

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(Map.of("message", message));
    }
}
