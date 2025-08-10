package app.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import app.ai.model.dto.request.AiRequest;
import app.ai.model.dto.response.AiResponse;
import app.ai.status.AiSuccessStatus;
import app.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

 @RestController
 @RequiredArgsConstructor
 @Tag(name = "AI", description = "AI 글쓰기 도우미 관련 API")
 @RequestMapping()
public class AiController {

private final AiService aiService;

 @PostMapping()
 @Operation(summary = "AI 글쓰기 도우미", description = "가게 또는 메뉴 설명을 AI를 통해 생성합니다.")
public ApiResponse<AiResponse> generateDescription(
 @RequestParam Long userId,
 @RequestBody @Valid AiRequest aiRequest) {
return ApiResponse.onSuccess(AiSuccessStatus.AI_RESPONDED, aiService.generateDescription(userId, aiRequest));

}

    @GetMapping("/chat")
    public String chat(@RequestParam("query") String query) {
        String aiResponse = aiService.getChatResponse(query);
        return "AI response: " + aiResponse;
    }
}
