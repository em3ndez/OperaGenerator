# Model Upgrade Session Notes
**Date:** 2026-01-23
**Branch:** `upgrade-models-to-latest`
**Status:** ✅ COMPLETE - All models upgraded and tested

## Objective
Upgrade all AI models to their latest versions and switch from OpenAI gpt-image-1 to Google Gemini Nano Banana (gemini-3-pro-image-preview) for image generation.

## Verified Model Names

### Chat Models (All Confirmed ✓)
- **Claude Opus 4.5**: `claude-opus-4-5-20251101` (production snapshot from Nov 1, 2025)
- **GPT-5.2**: `gpt-5.2` (standard - Note: gpt-5.2-pro does NOT exist)
- **Gemini 3 Flash**: `gemini-3-flash-preview` (fast)
- **Gemini 3 Pro**: `gemini-3-pro-preview` (advanced reasoning)

### Image Generation
- **Target**: `gemini-3-pro-image-preview` (Nano Banana Pro)
- **Library**: `com.google.genai:google-genai:1.36.0` (Google's native Java SDK)
- **Reason for switch**: User reports much better results with Nano Banana than gpt-image-1

## Environment Setup
- User has Pro Google account (needed for Nano Banana)
- Set `GOOGLE_API_KEY` environment variable (same value as existing `GOOGLEAI_API_KEY`)
- **Action needed**: Source ~/.zshrc to make GOOGLE_API_KEY visible to session

## Implementation Plan

### Task List (Completed)
1. ✅ **Task #1**: Add Google GenAI dependency to build.gradle.kts
2. ✅ **Task #2**: Update chat models to latest versions in AiModels.java
3. ✅ **Task #3**: Create GeminiImageGenerator.java using Google GenAI SDK
4. ✅ **Task #4**: Update IntegratedOperaGenerator to use GeminiImageGenerator
5. ✅ **Task #5**: Update documentation for new models and API keys

### Technical Details for GeminiImageGenerator

**API Pattern (from Google docs):**
```java
try (Client client = new Client()) {
  GenerateContentConfig config = GenerateContentConfig.builder()
      .responseModalities("IMAGE")
      .build();

  GenerateContentResponse response = client.models.generateContent(
      "gemini-3-pro-image-preview",
      "Your prompt here",
      config);

  // Extract image from response
  for (Part part : response.parts()) {
    if (part.inlineData().isPresent()) {
      var blob = part.inlineData().get();
      if (blob.data().isPresent()) {
        Files.write(Paths.get("output.png"), blob.data().get());
      }
    }
  }
}
```

**Architecture Requirements:**
- Maintain existing rate limiting with Semaphore (currently MAX_CONCURRENT=2, DELAY=1s)
- Use virtual threads for concurrent generation (same as current OperaImageGenerator)
- Keep same prompt structure for scene illustrations
- Save images with same naming: `scene_X_illustration.png`
- Handle errors gracefully with ImageGenerationException

**Key Differences from OpenAI:**
- Google SDK uses try-with-resources Client pattern
- Images returned as binary data in response parts (not base64 or URLs)
- Model name: `gemini-3-pro-image-preview` instead of `gpt-image-1`
- Authentication via GOOGLE_API_KEY env var (Client auto-detects)

## Files to Modify

### build.gradle.kts
Add dependency:
```kotlin
implementation("com.google.genai:google-genai:1.36.0")
```

### AiModels.java
Update model names:
- GPT_5 → `gpt-5.2`
- CLAUDE_OPUS_4_1 → `claude-opus-4-5-20251101`
- GEMINI_FLASH_25 → `gemini-3-flash-preview` or `gemini-3-pro-preview`

### GeminiImageGenerator.java (NEW FILE)
Create new class using Google GenAI SDK, replacing OperaImageGenerator functionality.

### IntegratedOperaGenerator.java
Step 4: Replace `OperaImageGenerator.generateImages(opera)` with `GeminiImageGenerator.generateImages(opera)`

### Documentation Updates
- README.md: Update model versions, add GOOGLE_API_KEY requirement
- CLAUDE.md: Update technical details about image generation
- Note Pro account requirement for Nano Banana

## Decision: Replace vs. Keep Both
**User preference**: Replace gpt-image-1 entirely with Nano Banana (better quality results)
- Can add OpenAI back as fallback later if needed
- Simplifies codebase by not maintaining two image generators

## Next Steps (After Environment Setup)
1. Verify GOOGLE_API_KEY is visible: `echo $GOOGLE_API_KEY`
2. Start with Task #1: Add dependency
3. Build project to verify dependency resolution
4. Proceed with implementation

## Resources
- [Google GenAI Maven Central](https://central.sonatype.com/artifact/com.google.genai/google-genai)
- [Google GenAI GitHub](https://github.com/googleapis/java-genai)
- [Gemini Image Generation Docs](https://ai.google.dev/gemini-api/docs/image-generation)
- [Claude Opus 4.5 Announcement](https://www.anthropic.com/news/claude-opus-4-5)
- [OpenAI GPT-5.2 Docs](https://platform.openai.com/docs/guides/latest-model)
