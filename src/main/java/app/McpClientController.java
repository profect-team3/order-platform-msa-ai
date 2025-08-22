package app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.commonSecurity.TokenPrincipalParser;
import app.model.dto.response.ChatResponse;
import app.status.AiSuccessStatus;
import app.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "MCP-Client", description = "사용자는 텍스트로 가게조회, 장바구니 담기, 주문을 할 수 있습니다.")
@RequestMapping()
@PreAuthorize("hasRole('CUSTOMER')")
public class McpClientController {

    private final ChatClient chatClient;
    private final TokenPrincipalParser tokenPrincipalParser;

    public McpClientController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools, ChatMemoryRepository chatMemoryRepository,
		TokenPrincipalParser tokenPrincipalParser) {

		ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(10)
            .build();

        this.chatClient = chatClientBuilder
            .defaultSystem("You are a helpful AI assistant that can use tools to search for stores, menus, add items to a cart, and create orders. Give short, concise answers.")
            .defaultToolCallbacks(tools)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
        this.tokenPrincipalParser = tokenPrincipalParser;
    }

    @GetMapping("/chat")
    public ApiResponse<ChatResponse> chat(
        Authentication authentication,
        @Parameter(description = "사용자를 식별하기 위한 유저 ID", required = true)
        @RequestParam String message) {

        String userId = tokenPrincipalParser.getUserId(authentication).toString();

        String response = chatClient.prompt()
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
            .user(message)
            .call()
            .content();

        return ApiResponse.onSuccess(AiSuccessStatus.MCP_CLIENT_SUCCESS, ChatResponse.builder().answer(response).build());
    }
}