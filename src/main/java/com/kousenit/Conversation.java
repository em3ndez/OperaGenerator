package com.kousenit;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Conversation {

    private static final Logger logger = LoggerFactory.getLogger(Conversation.class);

    private static final String DEFAULT_PREMISE = """
            They say that all operas are about a soprano
            who wants to sleep with the tenor, but the
            baritone won't let her. See, for example, La Traviata,
            Rigoletto, or Carmen.
            
            You are composing the libretto for such an opera.
            
            The setting is the wild jungles of Connecticut,
            in the not-so-distant future after global warming has
            reclaimed the land. The soprano is an intrepid
            explorer searching for the lost city of Hartford.
            The tenor is a native poet who has been living in
            the jungle for years, writing sonnets to the trees and
            composing symphonies for the monkeys.
            
            The baritone is a government agent who has been sent
            to stop the soprano from finding the lost city. He
            has a secret weapon: a giant robot that can sing
            Verdi arias in three different languages.
            
            The soprano and the tenor meet in the jungle and
            fall in love. They decide to join forces and find
            the lost city together. But the baritone is always
            one step behind them, and his giant robot is getting
            closer and closer.
            """;

    private static final Pattern SCENE_PATTERN = Pattern.compile("Scene\\s+(\\d+):\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);

    public static String defaultPremise() {
        return DEFAULT_PREMISE;
    }

    // Model configuration
    private static final double TEMPERATURE = 0.7;

    public final ChatModel gpt5 = AiModels.GPT_5_2;
    public final ChatModel claude = AiModels.CLAUDE_OPUS_4_5;

    public Opera generateOpera(String title, int numberOfScenes) {
        return generateOpera(title, DEFAULT_PREMISE, numberOfScenes);
    }

    public Opera generateOpera(String title, String premise, int numberOfScenes) {
        int memoryWindow = Math.max(numberOfScenes * 2 + 6, 20);
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(memoryWindow);
        List<Opera.Scene> scenes = new ArrayList<>();

        // First, ask for the opera title if not provided
        if (title == null || title.isBlank()) {
            memory.add(SystemMessage.from(premise));
            memory.add(UserMessage.from("""
                    Please suggest a creative and evocative title for this opera.
                    The title should capture the essence of the story - perhaps something about
                    the lost city of Hartford, the Connecticut jungle, or the romantic conflict.
                    Please provide just the title, nothing else.
                    """));
            ChatRequest titleRequest = ChatRequest.builder()
                .messages(memory.messages())
                .temperature(0.9) // Higher temperature for creative titles
                .build();

            ChatResponse titleResponse = gpt5.chat(titleRequest);
            title = extractTitle(titleResponse.aiMessage().text());
            logger.info("Generated opera title: {}", title);
            memory.clear();
        }

        // Set up the premise and global instructions for both collaborators
        memory.add(SystemMessage.from(premise + """
                
                We will collaboratively write %d scenes in total. Build toward a satisfying
                conclusion that resolves the opera in Scene %d. Maintain continuity with the
                context in memory and, if this session resumes mid-opera, continue from the
                next unwritten scene without retconning earlier events.
                
                When writing scenes:
                1. Format as: Scene X: [Scene Title]
                2. When introducing new characters, include their role/archetype
                   (e.g., "the young heroine", "the aging king", "the villainous count")
                3. If known, you may include voice type: CHARACTER (soprano), VILLAIN (baritone)
                4. This helps with proper operatic casting
                """.formatted(numberOfScenes, numberOfScenes)));

        // Use record for model info
        record ModelInfo(ChatModel model, String name) {}

        for (int i = 0; i < numberOfScenes; i++) {
            // Use switch expression for model selection
            ModelInfo modelInfo = switch (i % 2) {
                case 0 -> new ModelInfo(gpt5, "GPT-5");
                case 1 -> new ModelInfo(claude, "Claude Opus 4.1");
                default -> throw new IllegalStateException("Unexpected modulo result");
            };

            int sceneNumber = i + 1;
            boolean isFinalScene = sceneNumber == numberOfScenes;
            String scenePrompt = """
                    We are writing Scene %d of %d.
                    Provide only this scene with an evocative title and lyrical stage directions.
                    Maintain continuity with prior scenes captured in memory.
                    %s
                    """.formatted(
                    sceneNumber,
                    numberOfScenes,
                    isFinalScene
                            ? "This is the final scene; tie off the major plot threads, resolve the romantic arc, and deliver a decisive ending for the opera."
                            : "Set the stage for upcoming scenes so the opera can naturally progress to its finale.");

            memory.add(UserMessage.from(scenePrompt.trim()));

            // Use ChatRequest for better control
            // Note: maxOutputTokens removed - GPT-5.2 requires max_completion_tokens
            // which LangChain4j doesn't yet support. Relying on model defaults.
            ChatRequest sceneRequest = ChatRequest.builder()
                .messages(memory.messages())
                .temperature(TEMPERATURE)
                .build();

            logger.info("Generating Scene {} with {}", sceneNumber, modelInfo.name());
            ChatResponse response = modelInfo.model().chat(sceneRequest);
            String responseText = response.aiMessage().text();

            logger.debug("--------- {} ---------", modelInfo.name());
            logger.debug("Scene {} content: {}", sceneNumber,
                responseText.substring(0, Math.min(200, responseText.length())) + "...");

            // Parse the scene from the response
            Opera.Scene scene = parseScene(i + 1, responseText, modelInfo.name());
            scenes.add(scene);
            logger.info("Scene {} '{}' generated by {}",
                scene.number(), scene.title(), scene.author());

            memory.add(response.aiMessage());
        }

        logger.info("Opera '{}' generation complete with {} scenes", title, scenes.size());
        return new Opera(title, premise, scenes);
    }

    private String extractTitle(String response) {
        // Try to extract a quoted title or the first line
        Pattern titlePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = titlePattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Fallback: use first line or a default
        String[] lines = response.split("\n");
        return lines.length > 0 && !lines[0].isBlank()
                ? lines[0].replaceAll("[^\\w\\s]", "").trim()
                : "The Jungle Opera";
    }

    private Opera.Scene parseScene(int sceneNumber, String content, String author) {
        Matcher matcher = SCENE_PATTERN.matcher(content);

        if (matcher.find()) {
            // Use the scene number from the response if available
            int number = sceneNumber;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Use our counter if parsing fails
            }

            String title = matcher.group(2).trim();
            String sceneContent = content.substring(matcher.end()).trim();

            return new Opera.Scene(number, title, author, sceneContent);
        } else {
            // Fallback: treat the whole content as the scene
            String title = "Untitled Scene " + sceneNumber;
            return new Opera.Scene(sceneNumber, title, author, content);
        }
    }
}
