package br.edu.aquaflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cache e versionamento dos arquivos estáticos (application.properties). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StaticResourcesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void cssTemHashNoNomeECacheLongo() throws Exception {
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("/css/aquaflow-[0-9a-f]{32}\\.css").matcher(html);
        assertThat(matcher.find()).as("link do CSS com hash").isTrue();

        mvc.perform(get(matcher.group()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")));
    }
}
