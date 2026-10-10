package com.turtledot.studio.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    /**
     * Stores an uploaded file in the structured storage hierarchy:
     * /storage/studios/{studioId}/clients/{clientId}/events/{eventId}/albums/{albumId}/{type}/
     * Returns the relative or canonical stored file path.
     */
    String storeFile(Long studioId, Long clientId, Long eventId, Long albumId, String mediaType, String storedFileName, MultipartFile file) throws IOException;

    /**
     * Stores an input stream directly (e.g. for generated thumbnails/previews).
     */
    String storeStream(Long studioId, Long clientId, Long eventId, Long albumId, String subFolder, String storedFileName, InputStream inputStream) throws IOException;

    /**
     * Loads a file resource for streaming or download.
     */
    Resource loadAsResource(String filePath) throws IOException;

    /**
     * Deletes a specific file on disk.
     */
    boolean deleteFile(String filePath);

    /**
     * Safely deletes an album folder and all files contained within it.
     */
    boolean deleteAlbumDirectory(Long studioId, Long clientId, Long eventId, Long albumId);

    /**
     * Returns the root storage directory path.
     */
    Path getRootPath();
}
