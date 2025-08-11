package app;

import java.util.List;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.modelcontextprotocol.client.McpSyncClient;

@SpringBootApplication
public class AiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}
	// @Bean
	// public CommandLineRunner runChatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider,
	// 	List<McpSyncClient> mcpSyncClients) {
	// 	return args -> {
	// 		SyncMcpToolCallbackProvider toolCallbackProvider1 = new SyncMcpToolCallbackProvider(mcpSyncClients);
	// 		ToolCallback[] tools = toolCallbackProvider1.getToolCallbacks();
	//
	// 		System.out.println("\nRegistered MCP Tools:");
	// 		if (tools.length == 0) {
	// 			System.out.println("  - No tools found.");
	// 		} else {
	// 			for (ToolCallback tool : tools) {
	// 				System.out.println("  - Name: " + tool.getToolDefinition() + ", Description: " + tool.getToolMetadata());
	// 			}
	// 		}
	// 		System.out.println("----------------------------------------\n");
	//
	// 		var chatClient = chatClientBuilder
	// 			.defaultSystem("You are useful assistant and can perform web searches Brave's search API to reply to your questions.")
	// 			.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients))
	// 			.build();
	// 		System.out.println("\nI am your AI assistant.\n");
	// 		try (Scanner scanner = new Scanner(System.in)) {
	// 			while (true) {
	// 				System.out.print("\nUSER: ");
	// 				System.out.println("\nASSISTANT: " +
	// 					chatClient.prompt(scanner.nextLine())
	// 						.call()
	// 						.content());
	// 			}
	// 		}
	// 	};
	// }
	@Bean
	ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
		System.out.println("MCP Tools: " + toolCallbackProvider.getToolCallbacks());
		for (ToolCallback toolCallback : toolCallbackProvider.getToolCallbacks()) {
			System.out.println(" -----------"+ toolCallback);
		}
		return ChatClient
			.builder(chatModel)
			.defaultTools(toolCallbackProvider.getToolCallbacks())
			.build();
	}
}
