package com.demo.mcp_client.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

/**
 * @author Pramod Rajane
 */
@org.springframework.web.bind.annotation.RestController
public class RestController {

    private final ChatClient chatClient;

    public RestController(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {
        Arrays.stream(toolCallbackProvider.getToolCallbacks()).forEach(toolCallback -> {
            System.out.println("Tool Definition: " + toolCallback.getToolDefinition());
        });
        this.chatClient = builder
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .build();
    }

    @GetMapping("/getInfo")
    public String getInfo(@RequestParam("query") String query) {
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }
}
