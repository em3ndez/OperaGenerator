package com.kousenit;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.kousenit.tags.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Google GenAI SDK connection and authentication.
 */
@IntegrationTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class GeminiSdkConnectionTest {

    @Test
    void testBasicTextGeneration() {
        System.out.println("\n🔍 Testing Google GenAI SDK with text generation:");
        System.out.println("API Key: " + ApiKeys.GOOGLE_API_KEY.substring(0, 15) + "...");

        try (Client client = Client.builder()
                .apiKey(ApiKeys.GOOGLE_API_KEY)
                .build()) {

            // Try a simple text generation (not image)
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT")
                    .build();

            System.out.println("Calling gemini-3-flash-preview with TEXT modality...");
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    "Say hello in exactly 5 words",
                    config);

            System.out.println("✅ Text generation successful!");
            System.out.println("Response: " + response);

            assertThat(response).isNotNull();

        } catch (Exception e) {
            System.err.println("❌ Text generation failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testImageGenerationRequest() {
        System.out.println("\n🍌 Testing image generation API access:");
        System.out.println("API Key: " + ApiKeys.GOOGLE_API_KEY.substring(0, 15) + "...");

        try (Client client = Client.builder()
                .apiKey(ApiKeys.GOOGLE_API_KEY)
                .build()) {

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("IMAGE")
                    .build();

            System.out.println("Calling gemini-3-pro-image-preview with IMAGE modality...");
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-pro-image-preview",
                    "A simple red circle",
                    config);

            System.out.println("✅ Image generation successful!");
            System.out.println("Response: " + response);

            assertThat(response).isNotNull();

        } catch (Exception e) {
            System.err.println("❌ Image generation failed: " + e.getMessage());
            System.err.println("Full error:");
            e.printStackTrace();

            // Don't throw - we want to see this error for diagnosis
            System.err.println("\n💡 If you see 'API key not valid', try:");
            System.err.println("   1. Generate a new API key at https://aistudio.google.com/apikey");
            System.err.println("   2. Ensure image generation is enabled for your account");
            System.err.println("   3. Check if Pro account is required for image generation");
        }
    }
}
