package com.kousenit;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import com.kousenit.tags.IntegrationTest;
import com.kousenit.tags.ExpensiveTest;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ImageModelTest {

    private static final String TEST_PROMPT = """
            A steampunk robot playing a violin
            in a Victorian opera house,
            dramatic lighting, oil painting style
            """;

    @TempDir
    Path tempDir;


    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testGptImage1ModelWithBase64Output() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))  // Increase timeout for image generation
                .build();

        // When
        Response<Image> response = model.generate(TEST_PROMPT);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotNull();
        
        Image image = response.content();
        
        // Verify gpt-image-1 characteristics
        assertThat(image.base64Data())
                .as("gpt-image-1 should return base64 data")
                .isNotNull()
                .isNotEmpty();
        
        // gpt-image-1 might not return URLs (depending on implementation)
        System.out.printf("gpt-image-1 - Has base64: %s, Has URL: %s%n", 
                         image.base64Data() != null, image.url() != null);
        
        // Save the image first so we can see it even if other assertions fail
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        System.out.println("✅ gpt-image-1 test image saved to: " + savedPath);
        System.out.println("   Full path: " + savedPath.toAbsolutePath());
        
        // gpt-image-1 may not have revised prompt like DALL-E does
        if (image.revisedPrompt() != null) {
            System.out.println("Revised prompt: " + image.revisedPrompt());
        } else {
            System.out.println("No revised prompt returned (expected for gpt-image-1)");
        }
        
        // gpt-image-1 may not return token usage like DALL-E does
        if (response.tokenUsage() != null) {
            System.out.println("Token usage: " + response.tokenUsage());
        } else {
            System.out.println("No token usage returned (may be expected for gpt-image-1)");
        }
    }

    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testDallE3ModelWithUrlOutput() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();

        // When
        Response<Image> response = model.generate(TEST_PROMPT);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotNull();
        
        Image image = response.content();
        
        // Verify DALL-E 3 characteristics
        assertThat(image.url())
                .as("DALL-E 3 should return a URL")
                .isNotNull();
        
        System.out.printf("DALL-E 3 - Has base64: %s, Has URL: %s%n", 
                         image.base64Data() != null && !image.base64Data().isEmpty(), 
                         image.url() != null);
        
        assertThat(image.revisedPrompt())
                .as("Should have a revised prompt")
                .isNotNull();
        
        assertThat(response.tokenUsage())
                .as("Should have token usage info")
                .isNotNull();
    }

    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testImageSaverWithGptImage1() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .build();

        Response<Image> response = model.generate("A simple red circle on white background");

        // When
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());

        // Then
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        assertThat(savedPath.getFileName().toString())
                .as("Should be a PNG file")
                .endsWith(".png");
        
        System.out.println("✅ gpt-image-1 image saved to: " + savedPath.getFileName());
    }

    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testImageSaverWithDallE3() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();

        Response<Image> response = model.generate("A simple blue square on white background");

        // When
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());

        // Then
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        assertThat(savedPath.getFileName().toString())
                .as("Should be a PNG file")
                .endsWith(".png");
        
        System.out.println("✅ DALL-E 3 image saved to: " + savedPath.getFileName());
    }

    @Test
    @ExpensiveTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testModelComparison() {
        // This test compares the output characteristics of both models
        System.out.println("\n🔍 Model Comparison:");
        System.out.println("=" .repeat(50));
        
        // Test gpt-image-1
        var gptModel = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .build();
        
        Response<Image> gptResponse = gptModel.generate("A cat wearing a top hat");
        Image gptImage = gptResponse.content();
        
        // Test DALL-E 3
        var dalleModel = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();
        
        Response<Image> dalleResponse = dalleModel.generate("A cat wearing a top hat");
        Image dalleImage = dalleResponse.content();
        
        // Compare and report
        System.out.printf("gpt-image-1:  base64=%s, url=%s, tokens=%s%n",
                         gptImage.base64Data() != null && !gptImage.base64Data().isEmpty(),
                         gptImage.url() != null,
                         gptResponse.tokenUsage());
        
        System.out.printf("DALL-E 3:    base64=%s, url=%s, tokens=%s%n",
                         dalleImage.base64Data() != null && !dalleImage.base64Data().isEmpty(),
                         dalleImage.url() != null,
                         dalleResponse.tokenUsage());
        
        // Both should generate valid images
        assertThat(gptImage).isNotNull();
        assertThat(dalleImage).isNotNull();
    }

    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
    void testGeminiNanaBananaImageGeneration() throws Exception {
        System.out.println("\n🍌 Testing Gemini Nano Banana Image Generation:");
        System.out.println("=" .repeat(50));

        // Create a simple test opera with 2 scenes
        var scene1 = new Opera.Scene(
            1,
            "The Robot's Lament",
            "Gemini Nano Banana",
            """
            A melancholy robot stands alone on a rain-soaked stage,
            illuminated by a single spotlight. Its metallic frame reflects
            the blue lights of the theater as it contemplates its existence.
            """
        );

        var scene2 = new Opera.Scene(
            2,
            "Digital Dreams",
            "Gemini Nano Banana",
            """
            The robot's circuits glow with warm golden light as it experiences
            its first dream. Holographic butterflies dance around its head
            in a swirl of vibrant colors against a dark velvet backdrop.
            """
        );

        var testOpera = new Opera(
            "The Robot's Symphony",
            "A touching tale of a robot discovering emotions through opera",
            java.util.List.of(scene1, scene2)
        );

        // Temporarily set RESOURCE_PATH to temp directory for testing
        String originalPath = GeminiImageGenerator.RESOURCE_PATH;
        try {
            GeminiImageGenerator.RESOURCE_PATH = tempDir.toString();

            System.out.println("Generating images with Nano Banana...");
            System.out.println("This may take 1-2 minutes with rate limiting...\n");

            // Generate images
            GeminiImageGenerator.generateImages(testOpera);

            // Verify images were created
            Path image1 = tempDir.resolve(scene1.getImageFileName());
            Path image2 = tempDir.resolve(scene2.getImageFileName());

            assertThat(image1)
                .as("Scene 1 image should be created")
                .exists()
                .isRegularFile();

            assertThat(image2)
                .as("Scene 2 image should be created")
                .exists()
                .isRegularFile();

            System.out.println("✅ Scene 1 image created: " + image1.getFileName());
            System.out.println("   Full path: " + image1.toAbsolutePath());
            System.out.println("   File size: " + java.nio.file.Files.size(image1) + " bytes");

            System.out.println("\n✅ Scene 2 image created: " + image2.getFileName());
            System.out.println("   Full path: " + image2.toAbsolutePath());
            System.out.println("   File size: " + java.nio.file.Files.size(image2) + " bytes");

            System.out.println("\n🎉 Nano Banana test completed successfully!");
            System.out.println("View the generated images at: " + tempDir.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            GeminiImageGenerator.RESOURCE_PATH = originalPath;
        }
    }
}