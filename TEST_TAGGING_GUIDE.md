# Test Tagging Guide

This project uses JUnit 5 tags to categorize tests based on their cost and API usage.

## Test Categories

### 1. **Unit Tests** (No tags needed)
- No API calls
- Run on every push/PR
- Examples: `testOperaRecordMethods()`, file operation tests

### 2. **@IntegrationTest**
- Make API calls but are relatively inexpensive
- Run only on main branch pushes or manual trigger
- Examples:
  - Single image generation tests
  - Single text generation (critique)
  - Simple chat memory tests
  - Audio generation tests (2-3 files)

### 3. **@ExpensiveTest**
- Generate complete operas or multiple scenes
- Very high API costs
- Run only on manual trigger
- Examples:
  - `ContinueHartfordOperaTest` (3 scenes + 3 images)
  - `OperaGenerationIntegrationTest.testCompleteOperaGeneration()`
  - `ConversationTest.testGenerateSmallOpera()`

## How to Tag Your Tests

```java
import com.kousenit.tags.IntegrationTest;
import com.kousenit.tags.ExpensiveTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// For entire test class
@ExpensiveTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
class ContinueHartfordOperaTest {
    // All tests in this class are expensive
}

// For individual test methods
class ImageModelTest {
    @Test
    @IntegrationTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
    void testGptImage1ModelWithBase64Output() {
        // Single image generation - integration test
    }

    @Test
    @ExpensiveTest
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
    void testModelComparison() {
        // Multiple image generations - expensive test
    }
}
```

## Running Tests Locally

```bash
# Run only unit tests (no API calls)
./gradlew unitTest

# Run integration tests (cheaper API calls)
./gradlew integrationTest

# Run expensive tests (full opera generation)
./gradlew expensiveTest

# Run all tests
./gradlew test -Ptest.tags=integration,expensive
```

## GitHub Actions Workflow

- **Every Push/PR**: Runs `unitTest` only
- **Push to main**: Runs `unitTest` + `integrationTest`
- **Manual trigger**: Can run all tests including `expensiveTest`

## Cost Estimates

Based on typical usage:

| Test Type | Estimated Cost per Run | Frequency |
|-----------|------------------------|-----------|
| Unit Tests | $0 | Every push |
| Integration Tests | $0.10 - $0.50 | Main branch only |
| Expensive Tests | $1.00 - $5.00 | Manual only |

## Checklist for New Tests

1. [ ] Does the test make API calls?
   - No → No tag needed (unit test)
   - Yes → Continue to step 2

2. [ ] What type of API calls?
   - Single image/text/audio → Add `@IntegrationTest`
   - Full opera or 3+ scenes → Add `@ExpensiveTest`

3. [ ] Add `@EnabledIfEnvironmentVariable` for required API keys

4. [ ] Test locally with appropriate Gradle task

## Required GitHub Secrets

Configure these in your repository settings:
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `GOOGLEAI_API_KEY`
- `ELEVENLABS_API_KEY`