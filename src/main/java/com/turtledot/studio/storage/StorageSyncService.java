package com.turtledot.studio.storage;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.config.StorageProperties;
import com.turtledot.studio.media.Media;
import com.turtledot.studio.media.MediaRepository;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.storage.dto.StorageSyncReportDto;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class StorageSyncService {

    private static final Logger log = LoggerFactory.getLogger(StorageSyncService.class);

    private final Path rootPath;
    private final MediaRepository mediaRepository;
    private final AuditService auditService;

    public StorageSyncService(StorageProperties storageProperties,
                              MediaRepository mediaRepository,
                              AuditService auditService) {
        this.rootPath = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        this.mediaRepository = mediaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public StorageSyncReportDto scanStorage(UserPrincipal user) {
        Long studioId = user.getStudioId();
        StorageSyncReportDto report = new StorageSyncReportDto();

        List<Media> dbMedia = mediaRepository.findAll().stream()
                .filter(m -> m.getStudioId().equals(studioId))
                .toList();

        report.setTotalDbRecords(dbMedia.size());

        long totalDbBytes = 0;
        Set<String> knownFilePaths = new HashSet<>();

        // 1. Check for missing files in DB
        for (Media media : dbMedia) {
            totalDbBytes += media.getFileSize() != null ? media.getFileSize() : 0;
            String path = media.getFilePath();
            if (path != null) {
                File f = new File(path).getAbsoluteFile();
                knownFilePaths.add(f.getAbsolutePath());

                // Also account for thumbnails and previews
                String parent = f.getParent();
                if (parent != null) {
                    File thumbDir = new File(new File(parent).getParent(), "thumbnails");
                    if (thumbDir.exists() && thumbDir.isDirectory()) {
                        File[] thumbs = thumbDir.listFiles();
                        if (thumbs != null) {
                            for (File t : thumbs) {
                                knownFilePaths.add(t.getAbsolutePath());
                            }
                        }
                    }
                }

                if (!f.exists()) {
                    report.getMissingFiles().add(new StorageSyncReportDto.MissingFileDto(
                            media.getId(), media.getFileName(), path, media.getClientId(), media.getAlbumId()
                    ));
                }
            }
        }
        report.setTotalDbBytes(totalDbBytes);

        // 2. Scan physical disk directory
        Path studioDir = rootPath.resolve("studios").resolve(String.valueOf(studioId));
        long totalDiskBytes = 0;
        long totalDiskFiles = 0;

        if (Files.exists(studioDir)) {
            try (Stream<Path> stream = Files.walk(studioDir)) {
                List<Path> allPaths = stream.filter(Files::isRegularFile).toList();
                totalDiskFiles = allPaths.size();

                for (Path p : allPaths) {
                    File file = p.toFile();
                    long size = file.length();
                    totalDiskBytes += size;

                    String absPath = file.getAbsolutePath();
                    if (!knownFilePaths.contains(absPath)) {
                        String relPath = rootPath.relativize(p).toString();
                        report.getOrphanFiles().add(new StorageSyncReportDto.OrphanFileDto(
                                file.getName(), relPath, size, file.lastModified()
                        ));
                    }
                }
            } catch (IOException e) {
                log.error("Failed to scan physical studio directory: {}", studioDir, e);
            }
        }

        report.setTotalDiskFiles(totalDiskFiles);
        report.setTotalDiskBytes(totalDiskBytes);

        if (report.getMissingFiles().isEmpty() && report.getOrphanFiles().isEmpty()) {
            report.setStatus("HEALTHY");
        } else {
            report.setStatus("OUT_OF_SYNC");
        }

        return report;
    }

    public int pruneOrphans(UserPrincipal user) {
        Long studioId = user.getStudioId();
        StorageSyncReportDto report = scanStorage(user);
        int prunedCount = 0;

        for (StorageSyncReportDto.OrphanFileDto orphan : report.getOrphanFiles()) {
            Path target = rootPath.resolve(orphan.getRelativePath()).normalize();
            try {
                if (Files.exists(target) && target.startsWith(rootPath)) {
                    Files.delete(target);
                    prunedCount++;
                }
            } catch (IOException e) {
                log.warn("Could not delete orphan file: {}", target, e);
            }
        }

        auditService.log(
                studioId,
                user.getId(),
                "PRUNE_ORPHANS",
                "STORAGE",
                null,
                "Pruned " + prunedCount + " untracked orphan files from storage",
                null
        );

        return prunedCount;
    }
}
