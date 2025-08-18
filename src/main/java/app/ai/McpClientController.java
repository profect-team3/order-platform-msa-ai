package app.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.ai.model.dto.response.ChatResponse;
import app.ai.status.AiSuccessStatus;
import app.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "MCP-Client", description = "사용자는 텍스트로 가게조회, 장바구니 담기, 주문을 할 수 있습니다.")
@RequestMapping()
@PreAuthorize("hasRole('CUSTOMER')")
public class McpClientController {
	private final ChatClient chatClient;
	public McpClientController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools){
		this.chatClient = chatClientBuilder
			.defaultSystem("Please priorities context information for answering queries. Give short, coincide answer.")
			.defaultToolCallbacks(tools)
			.build();
	}
	@GetMapping("/chat")
	public ApiResponse<ChatResponse> chat(@RequestParam String message){
		PromptTemplate promptTemplate = new PromptTemplate(message);
		Prompt prompt = promptTemplate.create();
		ChatClient.CallResponseSpec res = chatClient.prompt(prompt).call();
		return ApiResponse.onSuccess(AiSuccessStatus.MCP_CLIENT_SUCCESS, ChatResponse.builder().answer(res.content()).build());
	}
}
