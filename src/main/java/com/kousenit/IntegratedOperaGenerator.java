package com.kousenit;

import java.io.IOException;
import java.nio.file.Path;

public class IntegratedOperaGenerator {

    public static void main(String[] args) {
        try {
            // Configuration
            String operaTitle = args.length > 0 ? args[0] : null; // Let AI suggest if not provided
            int numberOfScenes = args.length > 1 ? Integer.parseInt(args[1]) : 5;

            System.out.println("🎭 Starting Opera Generation Process...\n");

            // Step 1: Generate the opera with alternating AI models
            System.out.println("📝 Step 1: Generating opera scenes with GPT-4.1 and Claude Sonnet 4...");
            Conversation conversation = new Conversation();
            Opera opera = conversation.generateOpera(operaTitle, numberOfScenes);

            System.out.printf("\n✅ Generated opera: \"%s\" with %d scenes\n\n",
                    opera.title(), opera.scenes().size());

            // Step 2: Save complete opera with organized directory and formatting
            System.out.println("💾 Step 2: Saving complete opera with automatic stanza formatting...");
            var librettoPath = LibrettoWriter.saveCompleteOpera(opera);
            System.out.println("✅ Opera saved with beautiful formatting\n");

            // Step 4: Generate illustrations for each scene
            System.out.println("🎨 Step 4: Generating illustrations for each scene...");
            System.out.println("⚠️  This may take several minutes due to API rate limiting...\n");
            OperaImageGenerator.generateImages(opera);
            System.out.println("✅ Illustrations generated\n");

            // Final summary
            System.out.println("🎉 Opera Generation Complete!");
            System.out.println("=".repeat(50));
            System.out.printf("📖 Opera Title: %s%n", opera.title());
            System.out.printf("🎬 Total Scenes: %d%n", opera.scenes().size());
            System.out.printf("📁 Libretto File: %s%n", librettoPath.getFileName());
            System.out.println("🖼️  Individual scene files and illustrations created in src/main/resources/");

            // List the generated files
            System.out.println("\n📋 Generated Files:");
            opera.scenes().forEach(scene -> {
                System.out.printf("   • %s%n", scene.getFileName());
                System.out.printf("   • %s%n", scene.getImageFileName());
            });

            // Optional: Generate a critical review (requires a Google AI API key)
            if (System.getenv("GOOGLEAI_API_KEY") != null) {
                System.out.println("\n📰 Step 5: Generating critical review...");
                try {
                    OperaCritic critic = new OperaCritic();
                    Path operaDir = librettoPath.getParent();
                    critic.reviewAndSave(operaDir, opera.title());
                    System.out.println("✅ Critical review generated\n");
                } catch (Exception e) {
                    System.out.println("⚠️  Could not generate critique: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error during opera generation: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid number of scenes specified. Please provide a valid integer.");
            System.out.println("Usage: java IntegratedOperaGenerator [opera-title] [number-of-scenes]");
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
        }
    }
}