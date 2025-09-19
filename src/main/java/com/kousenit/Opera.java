package com.kousenit;

import java.util.List;
import java.util.Objects;

/**
 * Immutable record representing an opera with its scenes.
 */
public record Opera(
        String title,
        String premise,
        List<Scene> scenes
) {
    /**
     * Validates opera fields on construction.
     */
    public Opera {
        Objects.requireNonNull(title, "Opera title cannot be null");
        Objects.requireNonNull(premise, "Opera premise cannot be null");

        if (title.isBlank()) {
            throw new IllegalArgumentException("Opera title cannot be blank");
        }
        if (premise.isBlank()) {
            throw new IllegalArgumentException("Opera premise cannot be blank");
        }
        if (scenes == null || scenes.isEmpty()) {
            throw new IllegalArgumentException("Opera must have at least one scene");
        }

        // Make scenes list immutable
        scenes = List.copyOf(scenes);
    }

    /**
     * Immutable record representing a single scene in an opera.
     */
    public record Scene(
            int number,
            String title,
            String author,
            String content
    ) {
        /**
         * Validates scene fields on construction.
         */
        public Scene {
            if (number < 1) {
                throw new IllegalArgumentException("Scene number must be positive, got: " + number);
            }
            Objects.requireNonNull(title, "Scene title cannot be null");
            Objects.requireNonNull(author, "Scene author cannot be null");
            Objects.requireNonNull(content, "Scene content cannot be null");

            if (title.isBlank()) {
                throw new IllegalArgumentException("Scene title cannot be blank");
            }
            if (author.isBlank()) {
                throw new IllegalArgumentException("Scene author cannot be blank");
            }
            if (content.isBlank()) {
                throw new IllegalArgumentException("Scene content cannot be blank");
            }
        }

        /**
         * Gets the filename for this scene's text content.
         *
         * @return sanitized filename for the scene
         */
        public String getFileName() {
            String sanitizedTitle = title
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "_");
            return String.format("scene_%d_%s.txt", number, sanitizedTitle);
        }

        /**
         * Gets the filename for this scene's illustration.
         *
         * @return filename for the scene's image
         */
        public String getImageFileName() {
            return String.format("scene_%d_illustration.png", number);
        }
    }
}