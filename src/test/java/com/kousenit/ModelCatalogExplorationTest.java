package com.kousenit;

import dev.langchain4j.model.anthropic.AnthropicModelCatalog;
import dev.langchain4j.model.catalog.ModelDescription;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import dev.langchain4j.model.openai.OpenAiModelCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.kousenit.tags.IntegrationTest;

/**
 * Exploration test to understand the ModelCatalog feature in LangChain4j 1.10.0.
 * This helps us decide if we should refactor AiModels.java to use this new pattern.
 */
@IntegrationTest
class ModelCatalogExplorationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void exploreOpenAiModelCatalog() {
        System.out.println("\n📚 OpenAI Model Catalog:");
        System.out.println("=".repeat(60));

        var catalog = OpenAiModelCatalog.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .build();

        var models = catalog.listModels();
        System.out.println("Total models available: " + models.size());
        System.out.println();

        // Show GPT-5 models
        models.stream()
                .filter(m -> m.name().contains("gpt-5"))
                .forEach(m -> printModelInfo(m));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void exploreAnthropicModelCatalog() {
        System.out.println("\n📚 Anthropic Model Catalog:");
        System.out.println("=".repeat(60));

        var catalog = AnthropicModelCatalog.builder()
                .apiKey(ApiKeys.ANTHROPIC_API_KEY)
                .build();

        var models = catalog.listModels();
        System.out.println("Total models available: " + models.size());
        System.out.println();

        // Show Claude models
        models.stream()
                .filter(m -> m.name().contains("claude"))
                .forEach(m -> printModelInfo(m));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
    void exploreGoogleAiModelCatalog() {
        System.out.println("\n📚 Google AI Model Catalog:");
        System.out.println("=".repeat(60));

        var catalog = GoogleAiGeminiModelCatalog.builder()
                .apiKey(ApiKeys.GOOGLEAI_API_KEY)
                .build();

        var models = catalog.listModels();
        System.out.println("Total models available: " + models.size());
        System.out.println();

        // Show Gemini models
        models.stream()
                .filter(m -> m.name().contains("gemini"))
                .forEach(m -> printModelInfo(m));
    }

    private void printModelInfo(ModelDescription model) {
        System.out.println("Model: " + model.name());
        System.out.println("  Display Name: " + model.displayName());
        System.out.println("  Type: " + model.type());
        System.out.println("  Max Input Tokens: " + model.maxInputTokens());
        System.out.println("  Max Output Tokens: " + model.maxOutputTokens());
        if (model.createdAt() != null) {
            System.out.println("  Created: " + model.createdAt());
        }
        System.out.println();
    }
}
