package app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.commonUtil.apiPayload.exception.GeneralException;
import app.commonUtil.security.TokenPrincipalParser;
import app.model.AiHistoryRepository;
import app.model.dto.request.AiRequest;
import app.model.dto.response.AiResponse;
import app.model.entity.AiHistory;
import app.model.entity.enums.AiRequestStatus;
import app.model.entity.enums.ReqType;
import app.status.AiErrorStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.security.core.Authentication;

@DisplayName("AiService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @InjectMocks
    private AiService aiService;

    @Mock
    private AiHistoryRepository aiHistoryRepository;

    @Mock
    private ChatClient chatClient;

    @Mock
    private TokenPrincipalParser tokenPrincipalParser;

    @Mock
    private Authentication authentication;

    @Captor
    private ArgumentCaptor<AiHistory> aiHistoryCaptor;

    @Mock
    private ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private AiHistory savedHistory;

    @BeforeEach
    void setUp() {
        savedHistory = mock(AiHistory.class);
        lenient().when(savedHistory.getAiRequestId()).thenReturn(UUID.randomUUID());

        lenient().when(aiHistoryRepository.save(any(AiHistory.class))).thenReturn(savedHistory);

        lenient().when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.options(any(ChatOptions.class))).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);

        lenient().when(tokenPrincipalParser.getUserId(authentication)).thenReturn("1");
    }

    @Test
    @DisplayName("실패: 가게 이름 누락 시 예외 발생")
    void generateDescription_Fail_MissingStoreName() {
        AiRequest invalidRequest = new AiRequest(null, "메뉴", ReqType.MENU_DESCRIPTION, "요청사항");

        assertThatThrownBy(() -> aiService.generateDescription(authentication, invalidRequest))
            .isInstanceOf(GeneralException.class)
            .hasFieldOrPropertyWithValue("code", AiErrorStatus.AI_INVALID_INPUT_VALUE);

        verify(chatClient, never()).prompt();
        verify(aiHistoryRepository, never()).save(any(AiHistory.class));
    }

    @Test
    @DisplayName("실패: 메뉴 설명 요청에 메뉴 이름 누락 시 예외 발생")
    void generateDescription_Fail_MissingMenuNameForMenuDescription() {
        AiRequest invalidRequest = new AiRequest("가게", "", ReqType.MENU_DESCRIPTION, "요청사항");

        assertThatThrownBy(() -> aiService.generateDescription(authentication, invalidRequest))
            .isInstanceOf(GeneralException.class)
            .hasFieldOrPropertyWithValue("code", AiErrorStatus.AI_INVALID_INPUT_VALUE);

        verify(chatClient, never()).prompt();
        verify(aiHistoryRepository, never()).save(any(AiHistory.class));
    }

    @Test
    @DisplayName("성공: AI 요청 시 DB에 PENDING 상태로 저장")
    void generateDescription_Success_HistorySavedAsPending() {
        AiRequest aiRequest = new AiRequest("가게", "메뉴", ReqType.MENU_DESCRIPTION, "요청사항");
        when(callResponseSpec.content()).thenReturn("AI 응답");
        aiService.generateDescription(authentication, aiRequest);

        verify(aiHistoryRepository, times(1)).save(aiHistoryCaptor.capture());
        assertEquals(AiRequestStatus.PENDING, aiHistoryCaptor.getValue().getStatus());
        assertEquals("가게", aiHistoryCaptor.getValue().getStoreName());
    }

    @Test
    @DisplayName("성공: AI 응답 성공 시 DB 상태 SUCCESS로 업데이트")
    void generateDescription_Success_ResponseAndHistoryAreCorrect() {
        AiRequest aiRequest = new AiRequest("가게", "메뉴", ReqType.MENU_DESCRIPTION, "요청사항");
        String expectedContent = "AI의 멋진 응답입니다.";
        when(callResponseSpec.content()).thenReturn(expectedContent);

        doAnswer(invocation -> {
            when(savedHistory.getStatus()).thenReturn(AiRequestStatus.SUCCESS);
            when(savedHistory.getGeneratedContent()).thenReturn(expectedContent);
            return null;
        }).when(savedHistory).updateGeneratedContent(eq(expectedContent), eq(AiRequestStatus.SUCCESS));

        AiResponse response = aiService.generateDescription(authentication, aiRequest);

        assertNotNull(response);
        assertEquals(expectedContent, response.getGeneratedContent());

        verify(aiHistoryRepository, times(1)).save(any(AiHistory.class));
        verify(savedHistory).updateGeneratedContent(expectedContent, AiRequestStatus.SUCCESS);

        assertEquals(AiRequestStatus.SUCCESS, savedHistory.getStatus());
        assertEquals(expectedContent, savedHistory.getGeneratedContent());
    }

    @Test
    @DisplayName("실패: AI 응답 실패 시 예외 발생 및 DB 상태 FAILED로 업데이트")
    void generateDescription_Fail_ExceptionAndHistoryIsFailed() {
        AiRequest aiRequest = new AiRequest("가게", "메뉴", ReqType.MENU_DESCRIPTION, "요청사항");
        RuntimeException aiCallException = new RuntimeException("AI 모델 호출 실패");
        String errorMessage = "Error: " + aiCallException.getMessage();
        when(callResponseSpec.content()).thenThrow(aiCallException);

        assertThrows(GeneralException.class, () -> aiService.generateDescription(authentication, aiRequest));

        verify(aiHistoryRepository, times(1)).save(any(AiHistory.class));
        verify(savedHistory).updateGeneratedContent(errorMessage, AiRequestStatus.FAILED);
    }
}
