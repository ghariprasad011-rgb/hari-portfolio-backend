
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.PortfolioMedia;
import com.hari.portfolio_backend.entity.repository.PortfolioMediaRepository;
import com.hari.portfolio_backend.service.SupabaseStorageService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
public class PortfolioMediaController {

    private static final long MAX_FILE_SIZE =
            10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_FILES =
            Map.of(
                    "pdf", "application/pdf",
                    "jpg", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "png", "image/png",
                    "webp", "image/webp"
            );

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of("CERTIFICATE", "PHOTO", "PROJECT");

    private static final String FILE_NAME_PATTERN =
            "[a-fA-F0-9-]{36}\\.(pdf|jpg|jpeg|png|webp)";

    private final PortfolioMediaRepository mediaRepository;
    private final SupabaseStorageService storageService;

    public PortfolioMediaController(
            PortfolioMediaRepository mediaRepository,
            SupabaseStorageService storageService) {

        this.mediaRepository = mediaRepository;
        this.storageService = storageService;
    }

    // 1. ADMIN: LIST ALL MEDIA
    // GET /api/admin/media

    @GetMapping
    public List<PortfolioMedia> getAllMedia() {
        return mediaRepository.findAll();
    }

    // 2. PUBLIC: LIST APPROVED MEDIA
    // GET /api/admin/media/public

    @GetMapping("/public")
    public List<PortfolioMedia> getPublicMedia() {
        return mediaRepository.findByPublicVisibleTrue();
    }

    // 3. ADMIN: UPLOAD MEDIA
    // POST /api/admin/media

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
            return error(HttpStatus.BAD_REQUEST,
                    "Please choose a file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return error(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Maximum file size is 10 MB.");
        }

        if (title == null || title.isBlank()
                || title.length() > 200) {
            return error(HttpStatus.BAD_REQUEST,
                    "Enter a title between 1 and 200 characters.");
        }

        String normalizedCategory = category == null
                ? ""
                : category.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_CATEGORIES.contains(normalizedCategory)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid category.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            return error(HttpStatus.BAD_REQUEST,
                    "The uploaded file has no filename.");
        }

        String normalizedName = originalName.replace("\\", "/");

        String safeOriginalName = normalizedName.substring(
                normalizedName.lastIndexOf('/') + 1);

        int dot = safeOriginalName.lastIndexOf('.');

        if (dot < 1 || dot == safeOriginalName.length() - 1) {
            return error(HttpStatus.BAD_REQUEST,
                    "The file must have a supported extension.");
        }

        String extension = safeOriginalName.substring(dot + 1)
                .toLowerCase(Locale.ROOT);

        String expectedContentType = ALLOWED_FILES.get(extension);
        String actualContentType = file.getContentType();

        if (expectedContentType == null
                || actualContentType == null
                || !expectedContentType.equalsIgnoreCase(
                        actualContentType)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Unsupported file type or mismatched extension.");
        }

        String storedFileName = UUID.randomUUID()
                + "." + extension;

        boolean cloudUploaded = false;

        try {
            // Read file bytes and upload to Supabase.
            byte[] bytes = file.getBytes();

            storageService.upload(
                    storedFileName,
                    bytes,
                    expectedContentType);

            cloudUploaded = true;

            // Save only metadata in MySQL.
            PortfolioMedia media = new PortfolioMedia();

            media.setTitle(title.trim());
            media.setOriginalFileName(safeOriginalName);
            media.setStoredFileName(storedFileName);
            media.setContentType(expectedContentType);
            media.setCategory(normalizedCategory);
            media.setPublicVisible(publicVisible);

            // Existing frontend route remains unchanged.
            media.setFileUrl(
                    "/api/admin/media/public/files/"
                            + storedFileName);

            PortfolioMedia saved = mediaRepository.save(media);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(saved);

        } catch (IOException | RuntimeException exception) {

            // Compensate if the cloud upload succeeded but
            // saving metadata in MySQL failed.
            if (cloudUploaded) {
                try {
                    storageService.delete(storedFileName);
                } catch (IOException cleanupException) {
                    System.err.println(
                            "Cloud cleanup failed after upload error.");
                }
            }

            System.err.println(
                    "Portfolio media upload failed: "
                            + exception.getMessage());

            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upload failed. Check the Spring Boot logs.");
        }
    }

    // 4. ADMIN: VIEW ANY FILE
    // GET /api/admin/media/files/{filename}

    @GetMapping("/files/{storedFileName:.+}")
    public ResponseEntity<?> viewAdminMedia(
            @PathVariable String storedFileName) {

        return serveMedia(storedFileName, false);
    }

    // 5. PUBLIC: VIEW APPROVED FILES
    // GET /api/admin/media/public/files/{filename}

    @GetMapping("/public/files/{storedFileName:.+}")
    public ResponseEntity<?> viewPublicMedia(
            @PathVariable String storedFileName) {

        return serveMedia(storedFileName, true);
    }

    // 6. DOWNLOAD FROM PRIVATE SUPABASE BUCKET

    private ResponseEntity<?> serveMedia(
            String storedFileName,
            boolean publicOnly) {

        if (storedFileName == null
                || !storedFileName.matches(FILE_NAME_PATTERN)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid filename.");
        }

        PortfolioMedia media = mediaRepository.findAll()
                .stream()
                .filter(item -> storedFileName.equals(
                        item.getStoredFileName()))
                .findFirst()
                .orElse(null);

        if (media == null
                || (publicOnly && !media.isPublicVisible())) {
            return error(HttpStatus.NOT_FOUND,
                    "File not found.");
        }

        try {
            byte[] bytes = storageService.download(storedFileName);

            MediaType mediaType;

            try {
                mediaType = MediaType.parseMediaType(
                        media.getContentType());
            } catch (Exception exception) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(bytes.length)
                    .header("X-Content-Type-Options", "nosniff")
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + storedFileName + "\"")
                    .body(bytes);

        } catch (IOException exception) {
            System.err.println(
                    "Supabase file retrieval failed: "
                            + exception.getMessage());

            return error(HttpStatus.BAD_GATEWAY,
                    "Could not retrieve the file from cloud storage.");
        }
    }

    // 7. ADMIN: DELETE FILE AND DATABASE RECORD
    // DELETE /api/admin/media/{id}

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long id) {

        PortfolioMedia media = mediaRepository.findById(id)
                .orElse(null);

        if (media == null) {
            return error(HttpStatus.NOT_FOUND,
                    "The requested file was not found.");
        }

        String storedName = media.getStoredFileName();

        if (storedName == null
                || !storedName.matches(FILE_NAME_PATTERN)) {
            return error(HttpStatus.BAD_REQUEST,
                    "The stored filename is invalid.");
        }

        try {
            // Delete the cloud file first.
            storageService.delete(storedName);

            // Delete metadata only after cloud deletion succeeds.
            mediaRepository.delete(media);

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Cloud file and database record deleted."
            ));

        } catch (IOException | RuntimeException exception) {
            System.err.println(
                    "Portfolio media deletion failed: "
                            + exception.getMessage());

            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Deletion failed. Check server logs.");
        }
    }

    // 8. STANDARD ERROR RESPONSE

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message) {

        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }
}
