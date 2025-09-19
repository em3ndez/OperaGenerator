package com.kousenit;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the voice type detection logic in LibrettoWriter.
 */
class VoiceTypeDetectionTest {

    @Test
    void testExplicitVoiceTypes() throws Exception {
        assertVoiceType("ELENA (soprano)", "soprano");
        assertVoiceType("MARCUS (baritone)", "baritone");
        assertVoiceType("THE QUEEN (mezzo-soprano)", "mezzo-soprano");
        assertVoiceType("OLD WIZARD (bass)", "bass");
        assertVoiceType("YOUNG HERO (tenor)", "tenor");
        assertVoiceType("MARIA (alto)", "alto");
    }

    @Test
    void testArchetypeDetection() throws Exception {
        // Soprano archetypes
        assertVoiceType("ISABELLA, the young maiden", "soprano");
        assertVoiceType("PRINCESS AURORA", "soprano");
        assertVoiceType("THE HEROINE", "soprano");
        assertVoiceType("CLARA, the beloved daughter", "soprano");

        // Tenor archetypes
        assertVoiceType("PRINCE CHARMING", "tenor");
        assertVoiceType("THE POET", "tenor");
        assertVoiceType("ROMEO, the young lover", "tenor");
        assertVoiceType("THE HERO", "tenor");

        // Baritone archetypes
        assertVoiceType("COUNT DRACULA, the villain", "baritone");
        assertVoiceType("THE KING", "baritone");
        assertVoiceType("GENERAL SMITH", "baritone");
        assertVoiceType("GOVERNMENT AGENT", "baritone");

        // Bass archetypes
        assertVoiceType("THE ELDER", "bass");
        assertVoiceType("HIGH PRIEST", "bass");
        assertVoiceType("THE OLD SAGE", "bass");
        assertVoiceType("ANCIENT PROPHET", "bass");

        // Mezzo-soprano archetypes
        assertVoiceType("QUEEN ELIZABETH", "mezzo-soprano");
        assertVoiceType("THE MOTHER", "mezzo-soprano");
        assertVoiceType("THE WITCH", "mezzo-soprano");
    }

    @Test
    void testEnsembleDetection() throws Exception {
        assertVoiceType("CHORUS", "ensemble");
        assertVoiceType("ALL", "ensemble");
        assertVoiceType("CITIZENS OF HARTFORD", "ensemble");
        assertVoiceType("THE CROWD", "ensemble");
        assertVoiceType("VILLAGERS", "ensemble");
    }

    @Test
    void testGenericNames() throws Exception {
        // Should work with any character names, not just Hartford opera
        assertVoiceType("ELENA", "voice"); // Unknown name defaults to "voice"
        assertVoiceType("MARCUS", "voice");
        assertVoiceType("ALEXANDRA", "voice");

        // But with descriptors they should work
        assertVoiceType("ELENA, a young woman", "soprano");
        assertVoiceType("MARCUS, an older man", "baritone");
        assertVoiceType("ALEXANDRA, the heroine", "soprano");
    }

    @Test
    void testRobotAndAI() throws Exception {
        assertVoiceType("ROBOT-7", "bass");
        assertVoiceType("THE ANDROID", "bass");
        assertVoiceType("AI ASSISTANT", "bass");
        assertVoiceType("MECHANICAL GUARDIAN", "bass");
    }

    /**
     * Helper method to test voice type detection using reflection.
     */
    private void assertVoiceType(String character, String expectedVoiceType) throws Exception {
        // Use reflection to access the private determineVoiceType method
        Method method = LibrettoWriter.class.getDeclaredMethod("determineVoiceType", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, character);
        assertThat(result)
            .as("Voice type for '%s'", character)
            .isEqualTo(expectedVoiceType);
    }
}