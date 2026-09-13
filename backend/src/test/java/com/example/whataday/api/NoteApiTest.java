package com.example.whataday.api;

import com.example.whataday.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoteApiTest extends ApiTestSupport {

    @Test
    void createThenQueryByDate() throws Exception {
        String body = """
                {"noteTime":"2026-09-10T12:05:00","content":"写完优惠券领取逻辑","tags":["完成"]}
                """;

        mockMvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("写完优惠券领取逻辑"))
                .andExpect(jsonPath("$.data.tags[0]").value("完成"));

        mockMvc.perform(get("/api/notes").param("date", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].noteTime").value("2026-09-10T12:05:00"));
    }

    @Test
    void noteTimeDefaultsToNowWhenOmitted() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"临时想法\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noteTime").isNotEmpty())
                .andExpect(jsonPath("$.data.tags.length()").value(0));
    }

    @Test
    void blankContentIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("content")));
    }
}
