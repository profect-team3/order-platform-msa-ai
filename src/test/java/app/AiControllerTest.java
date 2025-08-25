package app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.model.dto.request.AiRequest;
import app.model.dto.response.AiResponse;
import app.model.entity.enums.ReqType;
import app.commonSecurity.TokenPrincipalParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private TokenPrincipalParser tokenPrincipalParser;

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1", roles = "OWNER")
    @DisplayName("AI 설명 생성 API 호출 성공")
    void generateDescription_Success() throws Exception {
        AiRequest request = new AiRequest("Test Store", "Test Menu", ReqType.MENU_DESCRIPTION, "Test Prompt");
        AiResponse response = new AiResponse("1", "Generated Description");

        when(aiService.generateDescription(any(), any(AiRequest.class))).thenReturn(response);

        mockMvc.perform(post("/describe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.generatedContent").value("Generated Description"));
    }

    @Test
    @DisplayName("AI 설명 생성 API 호출 실패 - 인증되지 않은 사용자")
    void generateDescription_Fail_Unauthorized() throws Exception {
        AiRequest request = new AiRequest("Test Store", "Test Menu", ReqType.MENU_DESCRIPTION, "Test Prompt");

        mockMvc.perform(post("/describe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
