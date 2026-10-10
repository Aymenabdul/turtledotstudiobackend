package com.turtledot.studio.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtledot.studio.album.dto.AlbumRequest;
import com.turtledot.studio.auth.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MediaUploadAndAlbumIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setupAdminToken() throws Exception {
        LoginRequest adminLogin = new LoginRequest("admin@studio.local", "Admin@123456");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin can create album, upload photo, stream media, and delete album safely")
    void testFullMediaLifecycle() throws Exception {
        // 1. Create Album for Event 1
        AlbumRequest albumReq = new AlbumRequest();
        albumReq.setEventId(1L);
        albumReq.setName("Sunset Portraits Test");
        albumReq.setDescription("Golden hour couple portraits");

        MvcResult albumResult = mockMvc.perform(post("/api/albums")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(albumReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sunset Portraits Test"))
                .andReturn();

        long albumId = objectMapper.readTree(albumResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Generate a small test JPEG in memory
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "sunset_test.jpg", "image/jpeg", imageBytes
        );

        // 3. Upload photo to the album
        MvcResult uploadResult = mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("albumId", String.valueOf(albumId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("sunset_test.jpg"))
                .andExpect(jsonPath("$.data.mediaType").value("PHOTO"))
                .andReturn();

        long mediaId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. Stream photo file
        mockMvc.perform(get("/api/media/" + mediaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        // 5. Delete album and verify clean removal
        mockMvc.perform(delete("/api/albums/" + albumId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
