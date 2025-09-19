package com.kousenit;

import com.kousenit.tags.ExpensiveTest;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quick test to check if we can handle higher concurrency for Tier 2.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
class QuickRateLimitTest {

    private static final Logger logger = LoggerFactory.getLogger(QuickRateLimitTest.class);

    @Test
    @ExpensiveTest
    void testTier2Concurrency() throws Exception {
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))
                .build();

        // Test with 8 concurrent requests (aggressive for Tier 2)
        int concurrency = 8;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        logger.info("Testing Tier 2 with {} concurrent requests", concurrency);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<CompletableFuture<Void>>();

            // Send all requests at once
            for (int i = 0; i < concurrency; i++) {
                final int requestNum = i + 1;
                var future = CompletableFuture.runAsync(() -> {
                    try {
                        String prompt = "Tier 2 test " + requestNum + ": A vibrant sunset over mountains";
                        Response<Image> response = model.generate(prompt);

                        if (response != null && response.content() != null) {
                            successCount.incrementAndGet();
                            logger.info("Request {} succeeded", requestNum);
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.warn("Request {} failed: {}", requestNum, e.getMessage());
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .orTimeout(5, TimeUnit.MINUTES)
                .join();

            Duration elapsed = Duration.between(start, Instant.now());

            logger.info("""

                ====================================
                Tier 2 Concurrency Test Results:
                ====================================
                Concurrent Requests: {}
                Successful: {}
                Failed: {}
                Time: {} seconds
                Success Rate: {}%

                Recommendation:
                """,
                concurrency,
                successCount.get(),
                errorCount.get(),
                elapsed.getSeconds(),
                (successCount.get() * 100.0 / concurrency)
            );

            if (errorCount.get() == 0) {
                logger.info("✅ EXCELLENT! All {} requests succeeded!", concurrency);
                logger.info("   You can safely use: OPERA_IMAGE_MAX_CONCURRENT={}", concurrency - 1);
                logger.info("   With minimal delay: OPERA_IMAGE_DELAY_MS=100");
            } else if (successCount.get() >= 6) {
                logger.info("✅ GOOD! Most requests succeeded.");
                logger.info("   Safe setting: OPERA_IMAGE_MAX_CONCURRENT={}", successCount.get() - 1);
                logger.info("   With small delay: OPERA_IMAGE_DELAY_MS=250");
            } else if (successCount.get() >= 4) {
                logger.info("⚠️  MODERATE. Some failures detected.");
                logger.info("   Conservative setting: OPERA_IMAGE_MAX_CONCURRENT={}", successCount.get());
                logger.info("   With delay: OPERA_IMAGE_DELAY_MS=500");
            } else {
                logger.info("❌ LIMITED. Many failures.");
                logger.info("   Stick with: OPERA_IMAGE_MAX_CONCURRENT=3");
                logger.info("   With delay: OPERA_IMAGE_DELAY_MS=1000");
            }

            // Assert that at least half succeeded
            assertThat(successCount.get())
                .as("At least half of requests should succeed for Tier 2")
                .isGreaterThanOrEqualTo(concurrency / 2);
        }
    }
}