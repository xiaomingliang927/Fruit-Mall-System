package com.fruitmall.upload;

import com.fruitmall.auth.JwtUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 凭证图上传闭环：格式校验 / 落盘 / 5MB 上限（Spring 默认仅 1MB，已在 yml 中放开） */
@SpringBootTest(properties = "app.upload.dir=${java.io.tmpdir}/fruit-upload-test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadImageIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtUtil jwtUtil;

    private String auth() {
        return "Bearer " + jwtUtil.generate(1L);
    }

    /** 返回响应体（按 UTF-8 读取，避免中文乱码） */
    private String upload(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/v1/upload/image").file(file).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("上传 jpg：返回 /uploads/ 访问路径且文件已落盘")
    void uploadJpg() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad-fruit.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9});

        String body = upload(file);
        assertThat((Integer) JsonPath.read(body, "$.code")).isZero();

        String url = JsonPath.read(body, "$.data.url");
        assertThat((String) url).startsWith("/uploads/");
        Path disk = Path.of(System.getProperty("java.io.tmpdir"), "fruit-upload-test",
                url.substring("/uploads/".length()));
        assertThat(Files.exists(disk)).isTrue();
    }

    @Test
    @DisplayName("4MB 图片可上传：multipart 上限已放开（默认 1MB 会被容器拦截）")
    void uploadUnder5Mb() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[4 * 1024 * 1024]);

        String body = upload(file);
        assertThat((Integer) JsonPath.read(body, "$.code")).isZero();
    }

    @Test
    @DisplayName("非图片格式：返回可读的业务错误提示")
    void rejectNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "not an image".getBytes(StandardCharsets.UTF_8));

        String body = upload(file);
        assertThat((Integer) JsonPath.read(body, "$.code")).isEqualTo(1000);
        assertThat((String) JsonPath.read(body, "$.message")).contains("仅支持");
    }
}
