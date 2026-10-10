package com.turtledot.studio.storage;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    public static class ImageDimensions {
        public final int width;
        public final int height;

        public ImageDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public ImageDimensions extractDimensions(File imageFile) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img != null) {
                return new ImageDimensions(img.getWidth(), img.getHeight());
            }
        } catch (Exception e) {
            log.warn("Could not read image dimensions from {}: {}", imageFile.getName(), e.getMessage());
        }
        return new ImageDimensions(1920, 1080);
    }

    public byte[] generateThumbnail(File sourceFile, int width, int height) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(sourceFile)
                .size(width, height)
                .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toOutputStream(outputStream);
        return outputStream.toByteArray();
    }

    public byte[] generateWebPreview(File sourceFile, int maxWidth, int maxHeight) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(sourceFile)
                .size(maxWidth, maxHeight)
                .outputFormat("jpg")
                .outputQuality(0.88)
                .toOutputStream(outputStream);
        return outputStream.toByteArray();
    }
}
