package app.ai;

import app.ai.model.dto.request.AiRequest;
import app.ai.model.dto.response.AiResponse;

public interface AiService {
	AiResponse generateDescription(Long userId, AiRequest aiRequest);

	String getChatResponse(String query);
}
