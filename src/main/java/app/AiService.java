package app;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import app.commonUtil.apiPayload.exception.GeneralException;
import app.commonUtil.security.TokenPrincipalParser;
import app.model.AiHistoryRepository;
import app.model.dto.request.AiRequest;
import app.model.dto.response.AiResponse;
import app.model.entity.AiHistory;
import app.model.entity.enums.AiRequestStatus;
import app.model.entity.enums.ReqType;
import app.status.AiErrorStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
@lombok.extern.slf4j.Slf4j
public class AiService{

	private final AiHistoryRepository aiHistoryRepository;
	private final ChatClient chatClient;
	private final TokenPrincipalParser tokenPrincipalParser;

	public AiResponse generateDescription(Authentication authentication, AiRequest aiRequest) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		if (StringUtils.hasText(aiRequest.getStoreName())) {
			if (aiRequest.getReqType() == ReqType.MENU_DESCRIPTION && !StringUtils.hasText(aiRequest.getMenuName())) {
				throw new GeneralException(AiErrorStatus.AI_INVALID_INPUT_VALUE);
			}

			AiHistory aiRequestEntity = AiHistory.builder()
				.userId(userId)
				.storeName(aiRequest.getStoreName())
				.menuName(StringUtils.hasText(aiRequest.getMenuName()) ? aiRequest.getMenuName() : "")
				.reqType(aiRequest.getReqType())
				.promptText(aiRequest.getPromptText())
				.status(AiRequestStatus.PENDING)
				.build();

			AiHistory savedAiRequestEntity = aiHistoryRepository.save(aiRequestEntity);
			PromptTemplate promptTemplate = new PromptTemplate("""
				너는 사용자의 요청에 맞춰 배달앱에 적합한 마케팅 문구를 생성하는 AI야. 아래 주어진 정보를 바탕으로 멋진 결과물을 만들어줘.
				
				- 가게 이름: {storeName}
				- 메뉴 이름: {menuName}
				- 요청 종류: {reqType}
				- 핵심 요청사항 : {promptText}
				
				요청 종류가 MENU_DESCRIPTION 이면 30자 이내로 작성해주고 STORE_DESCRIPTION 이면 100자 이내로 작성해줘.
				""");
			Prompt prompt = promptTemplate.create(Map.of(
				"storeName", aiRequestEntity.getStoreName(), "menuName", aiRequestEntity.getMenuName()
				, "reqType", aiRequestEntity.getReqType(), "promptText", aiRequestEntity.getPromptText())
			);
			String generatedContent;
			try {
				log.info("Request to OpenAI: {}", prompt.getContents());

				generatedContent = chatClient.prompt()
					.options(OpenAiChatOptions.builder().model("gpt-4.1-mini").build())
					.user(prompt.getContents())
					.call()
					.content();
				savedAiRequestEntity.updateGeneratedContent(generatedContent, AiRequestStatus.SUCCESS);
			} catch (Exception e) {
				savedAiRequestEntity.updateGeneratedContent("Error: " + e.getMessage(), AiRequestStatus.FAILED);
				throw new GeneralException(AiErrorStatus.AI_GENERATION_FAILED);
			}

			return new AiResponse(savedAiRequestEntity.getAiRequestId().toString(), generatedContent);
		} else {
			throw new GeneralException(AiErrorStatus.AI_INVALID_INPUT_VALUE);
		}
	}
}