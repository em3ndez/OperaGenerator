package com.kousenit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductionRunFormatter {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: ProductionRunFormatter <production_run_directory>");
            return;
        }

        Path runDir = Path.of(args[0]);
        if (!Files.isDirectory(runDir)) {
            System.err.println("Directory not found: " + runDir);
            return;
        }

        Path metadataPath = runDir.resolve("production_metadata.json");
        if (!Files.exists(metadataPath)) {
            System.err.println("production_metadata.json not found in " + runDir);
            return;
        }

        String metadata = Files.readString(metadataPath);

        String title = extractString(metadata, "title");
        String premise = unescapeJsonString(extractString(metadata, "premise"));

        List<Opera.Scene> scenes = extractScenes(metadata, runDir);
        scenes.sort(Comparator.comparingInt(Opera.Scene::number));

        if (title == null || premise == null || scenes.isEmpty()) {
            System.err.println("Failed to parse production metadata; aborting");
            return;
        }

        String originalResourcePath = LibrettoWriter.RESOURCE_PATH;
        try {
            LibrettoWriter.RESOURCE_PATH = runDir.toString();
            Opera opera = new Opera(title, premise, scenes);
            LibrettoWriter.saveCompleteOpera(opera);
            LibrettoWriter.saveScenesToFiles(opera);
            System.out.println("Reformatted libretto and scene files for " + title);
        } finally {
            LibrettoWriter.RESOURCE_PATH = originalResourcePath;
        }
    }

    private static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static List<Opera.Scene> extractScenes(String json, Path runDir) throws IOException {
        List<Opera.Scene> scenes = new ArrayList<>();
        Pattern scenePattern = Pattern.compile(
                "\\{\\s*\"number\"\\s*:\\s*(\\d+),\\s*\"title\"\\s*:\\s*\"(.*?)\",\\s*\"author\"\\s*:\\s*\"(.*?)\",\\s*\"sceneFile\"\\s*:\\s*\"(.*?)\"",
                Pattern.DOTALL);
        Matcher matcher = scenePattern.matcher(json);
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String title = matcher.group(2);
            String author = matcher.group(3);
            String sceneFile = matcher.group(4);

            Path scenePath = runDir.resolve(sceneFile);
            if (!Files.exists(scenePath)) {
                System.err.println("Scene file missing: " + scenePath);
                continue;
            }

            String content = Files.readString(scenePath).trim();
            String formattedContent = extractSceneBody(content);
            scenes.add(new Opera.Scene(number, title, author, formattedContent));
        }
        return scenes;
    }

    private static String extractSceneBody(String rawContent) {
        String[] lines = rawContent.split("\n");
        StringBuilder body = new StringBuilder();
        boolean pastHeader = false;
        for (String line : lines) {
            if (!pastHeader) {
                if (line.trim().isEmpty()) {
                    pastHeader = true;
                }
                continue;
            }
            body.append(line).append('\n');
        }
        return body.toString().trim();
    }

    private static String unescapeJsonString(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\u0027", "'")
                .replace("\\u2019", "’");
    }
}
