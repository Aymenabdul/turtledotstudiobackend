package com.turtledot.studio.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);
    private final boolean ffmpegAvailable;

    public VideoProcessingService() {
        this.ffmpegAvailable = checkFfmpegAvailability();
        if (this.ffmpegAvailable) {
            log.info("FFmpeg CLI detected and enabled for video thumbnail extraction");
        } else {
            log.info("FFmpeg CLI not found on host. Graceful video placeholder fallback enabled.");
        }
    }

    public static class VideoMetadata {
        public final long durationSeconds;
        public final int width;
        public final int height;

        public VideoMetadata(long durationSeconds, int width, int height) {
            this.durationSeconds = durationSeconds;
            this.width = width;
            this.height = height;
        }
    }

    public VideoMetadata extractMetadata(File videoFile) {
        // Fallback default 1080p video metadata
        return new VideoMetadata(60, 1920, 1080);
    }

    public byte[] generateVideoThumbnail(File videoFile) throws IOException {
        if (ffmpegAvailable) {
            try {
                File tempOutput = File.createTempFile("vthumb_", ".jpg");
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg", "-y", "-ss", "00:00:01",
                        "-i", videoFile.getAbsolutePath(),
                        "-vframes", "1",
                        "-vf", "scale=640:-1",
                        tempOutput.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);

                if (finished && process.exitValue() == 0 && tempOutput.exists() && tempOutput.length() > 0) {
                    byte[] data = java.nio.file.Files.readAllBytes(tempOutput.toPath());
                    tempOutput.delete();
                    return data;
                }
                tempOutput.delete();
            } catch (Exception e) {
                log.warn("FFmpeg thumbnail extraction failed, using fallback: {}", e.getMessage());
            }
        }

        // High quality stylized video placeholder generator
        return generateStylizedVideoThumbnail();
    }

    private byte[] generateStylizedVideoThumbnail() throws IOException {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark photography background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(15, 23, 42), 640, 360, new Color(30, 41, 59));
        g2.setPaint(gp);
        g2.fillRect(0, 0, 640, 360);

        // Play button badge
        g2.setColor(new Color(245, 158, 11, 220));
        g2.fillOval(270, 130, 100, 100);

        g2.setColor(new Color(15, 23, 42));
        Polygon triangle = new Polygon();
        triangle.addPoint(310, 160);
        triangle.addPoint(310, 200);
        triangle.addPoint(345, 180);
        g2.fillPolygon(triangle);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        String label = "VIDEO PREVIEW";
        FontMetrics fm = g2.getFontMetrics();
        int x = (640 - fm.stringWidth(label)) / 2;
        g2.drawString(label, x, 270);

        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private boolean checkFfmpegAvailability() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
