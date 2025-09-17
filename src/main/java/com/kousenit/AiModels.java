package com.kousenit;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

@SuppressWarnings("unused")
public class AiModels {
    public static final ChatModel GPT_5 = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName("gpt-5")
            .timeout(Duration.ofMinutes(2))
            .maxRetries(4)
            .build();

    public static final ChatModel CLAUDE_OPUS_4_1 = AnthropicChatModel.builder()
            .apiKey(ApiKeys.ANTHROPIC_API_KEY)
            .modelName("claude-opus-4-1-20250805")
            .logRequests(true)
            .logResponses(true)
            .build();

    public static final ChatModel GPT_5_MINI = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName("gpt-5-mini")
            .timeout(Duration.ofMinutes(2))
            .maxRetries(4)
            .build();

    public static final ChatModel GEMINI_FLASH_25 = GoogleAiGeminiChatModel.builder()
            .apiKey(ApiKeys.GOOGLEAI_API_KEY)
            .modelName("gemini-2.5-pro")
            .build();
}
