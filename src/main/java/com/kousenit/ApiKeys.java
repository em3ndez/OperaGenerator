package com.kousenit;

/**
 * Centralized API key management with validation.
 * Validates required keys at class loading time.
 */
public class ApiKeys {
    public static final String OPENAI_API_KEY;
    public static final String ANTHROPIC_API_KEY;
    public static final String GOOGLEAI_API_KEY;
    public static final String ELEVENLABS_API_KEY;

    static {
        // Required keys - will throw exception if missing
        OPENAI_API_KEY = requireApiKey("OPENAI_API_KEY");
        ANTHROPIC_API_KEY = requireApiKey("ANTHROPIC_API_KEY");

        // Optional keys - can be null
        GOOGLEAI_API_KEY = getOptionalApiKey("GOOGLEAI_API_KEY");
        ELEVENLABS_API_KEY = getOptionalApiKey("ELEVENLABS_API_KEY");
    }

    /**
     * Validates that a required API key is present in environment variables.
     *
     * @param keyName the name of the environment variable
     * @return the API key value
     * @throws IllegalStateException if the key is missing or blank
     */
    private static String requireApiKey(String keyName) {
        String key = System.getenv(keyName);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                String.format("Required environment variable %s is missing or blank. " +
                            "Please set it before running the application.", keyName)
            );
        }
        return key;
    }

    /**
     * Gets an optional API key from environment variables.
     *
     * @param keyName the name of the environment variable
     * @return the API key value or null if not present
     */
    private static String getOptionalApiKey(String keyName) {
        String key = System.getenv(keyName);
        if (key != null && !key.isBlank()) {
            System.out.println("✓ " + keyName + " is configured");
            return key;
        }
        System.out.println("ℹ️ " + keyName + " is not configured (optional)");
        return null;
    }

    /**
     * Checks if an optional API key is available.
     *
     * @param key the API key to check
     * @return true if the key is not null and not blank
     */
    public static boolean isAvailable(String key) {
        return key != null && !key.isBlank();
    }
}
