package com.turtledot.studio.storage;

import com.turtledot.studio.config.StorageProperties;
import com.turtledot.studio.exception.StorageException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootPath;

    public LocalStorageService(StorageProperties storageProperties) {
        this.rootPath = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("Storage root initialized at: {}", this.rootPath);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeFile(Long studioId, Long clientId, Long eventId, Long albumId, String mediaType, String storedFileName, MultipartFile file) throws IOException {
        String subFolder = "photos";
        if ("VIDEO".equalsIgnoreCase(mediaType)) {
            subFolder = "videos";
        }

        Path targetDir = buildAlbumPath(studioId, clientId, eventId, albumId, subFolder);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(storedFileName).normalize();
        validatePathTraversal(targetFile);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return targetFile.toString();
    }

    @Override
    public String storeStream(Long studioId, Long clientId, Long eventId, Long albumId, String subFolder, String storedFileName, InputStream inputStream) throws IOException {
        Path targetDir = buildAlbumPath(studioId, clientId, eventId, albumId, subFolder);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(storedFileName).normalize();
        validatePathTraversal(targetFile);

        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return targetFile.toString();
    }

    @Override
    public Resource loadAsResource(String filePath) throws IOException {
        Path path = Paths.get(filePath).normalize();
        validatePathTraversal(path);

        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new StorageException("File not found or not readable: " + filePath);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            validatePathTraversal(path);
            return Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete physical file {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteAlbumDirectory(Long studioId, Long clientId, Long eventId, Long albumId) {
        try {
            Path albumDir = rootPath.resolve(String.format("studios/%d/clients/%d/events/%d/albums/%d",
                    studioId, clientId, eventId, albumId)).normalize();
            validatePathTraversal(albumDir);

            File dirFile = albumDir.toFile();
            if (dirFile.exists() && dirFile.isDirectory()) {
                FileUtils.deleteDirectory(dirFile);
                log.info("Successfully deleted physical album directory: {}", albumDir);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to delete physical album directory for album {}: {}", albumId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Path getRootPath() {
        return rootPath;
    }

    private Path buildAlbumPath(Long studioId, Long clientId, Long eventId, Long albumId, String subFolder) {
        return rootPath.resolve(String.format("studios/%d/clients/%d/events/%d/albums/%d/%s",
                studioId, clientId, eventId, albumId, subFolder)).normalize();
    }

    private void validatePathTraversal(Path target) {
        try {
            String canonicalTarget = target.toFile().getCanonicalPath();
            String canonicalRoot = rootPath.toFile().getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalRoot)) {
                throw new StorageException("Security violation: Path traversal attempt detected outside storage root");
            }
        } catch (IOException e) {
            throw new StorageException("Failed to validate file path canonical form", e);
        }
    }
}
