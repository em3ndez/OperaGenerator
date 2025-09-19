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
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to explore actual rate limits for gpt-image-1 model.
 * This test helps determine optimal concurrency settings.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
class RateLimitExplorationTest {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitExplorationTest.class);

    @Test
    @ExpensiveTest
    void exploreRateLimitsWithIncreasingConcurrency() throws Exception {
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))
                .build();

        // Test with different concurrency levels
        int[] concurrencyLevels = {1, 2, 3, 4, 5, 8, 10};

        for (int concurrency : concurrencyLevels) {
            logger.info("Testing with {} concurrent requests", concurrency);
            testWithConcurrency(model, concurrency);

            // Wait between tests to reset any rate limit windows
            Thread.sleep(5000);
        }
    }

    private void testWithConcurrency(OpenAiImageModel model, int maxConcurrent) throws Exception {
        Semaphore rateLimiter = new Semaphore(maxConcurrent);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        int totalRequests = Math.min(maxConcurrent * 2, 10); // Test with 2x concurrency or max 10
        CountDownLatch latch = new CountDownLatch(totalRequests);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalRequests; i++) {
                final int requestNum = i + 1;
                executor.submit(() -> {
                    try {
                        rateLimiter.acquire();
                        try {
                            logger.debug("Request {} starting", requestNum);
                            String prompt = String.format(
                                "A simple test image %d: Abstract geometric shapes in blue and green",
                                requestNum
                            );

                            Response<Image> response = model.generate(prompt);

                            if (response != null && response.content() != null) {
                                successCount.incrementAndGet();
                                logger.debug("Request {} succeeded", requestNum);
                            } else {
                                errorCount.incrementAndGet();
                                errors.add("Request " + requestNum + ": null response");
                            }
                        } finally {
                            rateLimiter.release();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        String errorMsg = "Request " + requestNum + ": " + e.getMessage();
                        errors.add(errorMsg);
                        logger.warn("Request {} failed: {}", requestNum, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all requests to complete
            boolean completed = latch.await(5, TimeUnit.MINUTES);

            Duration elapsed = Duration.between(start, Instant.now());

            logger.info("""
                Concurrency Level: {}
                Total Requests: {}
                Successful: {}
                Failed: {}
                Time Elapsed: {} seconds
                Requests per second: {:.2f}
                """,
                maxConcurrent,
                totalRequests,
                successCount.get(),
                errorCount.get(),
                elapsed.getSeconds(),
                totalRequests / (double) elapsed.getSeconds()
            );

            if (!errors.isEmpty()) {
                logger.info("Errors encountered:");
                errors.forEach(error -> logger.info("  - {}", error));
            }

            // Assert that at least some requests succeeded
            assertThat(successCount.get())
                .as("At least one request should succeed with concurrency " + maxConcurrent)
                .isGreaterThan(0);
        }
    }

    @Test
    @ExpensiveTest
    void testBurstRequests() throws Exception {
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))
                .build();

        int burstSize = 5;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        logger.info("Testing burst of {} simultaneous requests", burstSize);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<CompletableFuture<Void>>();

            // Send all requests at once (no rate limiting)
            for (int i = 0; i < burstSize; i++) {
                final int requestNum = i + 1;
                var future = CompletableFuture.runAsync(() -> {
                    try {
                        String prompt = "Burst test image " + requestNum + ": A serene landscape";
                        Response<Image> response = model.generate(prompt);

                        if (response != null && response.content() != null) {
                            successCount.incrementAndGet();
                            logger.info("Burst request {} succeeded", requestNum);
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.warn("Burst request {} failed: {}", requestNum, e.getMessage());
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
                Burst Test Results:
                Burst Size: {}
                Successful: {}
                Failed: {}
                Time: {} seconds
                Success Rate: {}%
                """,
                burstSize,
                successCount.get(),
                errorCount.get(),
                elapsed.getSeconds(),
                (successCount.get() * 100.0 / burstSize)
            );

            // Provide recommendation based on results
            if (errorCount.get() == 0) {
                logger.info("✅ All {} requests succeeded! Consider increasing MAX_CONCURRENT_REQUESTS", burstSize);
            } else if (successCount.get() > 0) {
                logger.info("⚠️ Mixed results. Safe concurrency appears to be around {}", successCount.get());
            } else {
                logger.info("❌ All requests failed. Current limits may be more restrictive than expected");
            }
        }
    }

    @Test
    @ExpensiveTest
    void testWithDelayBetweenRequests() throws Exception {
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))
                .build();

        // Test different delays
        long[] delaysMs = {0, 250, 500, 1000};

        for (long delayMs : delaysMs) {
            logger.info("Testing with {}ms delay between requests", delayMs);

            int successCount = 0;
            int totalRequests = 5;

            for (int i = 0; i < totalRequests; i++) {
                try {
                    String prompt = "Delay test " + i + ": A peaceful garden scene";
                    Response<Image> response = model.generate(prompt);

                    if (response != null && response.content() != null) {
                        successCount++;
                        logger.debug("Request {} with {}ms delay succeeded", i + 1, delayMs);
                    }

                    if (i < totalRequests - 1 && delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                } catch (Exception e) {
                    logger.warn("Request failed with {}ms delay: {}", delayMs, e.getMessage());
                }
            }

            logger.info("Delay {}ms: {}/{} requests succeeded", delayMs, successCount, totalRequests);

            // Wait between different delay tests
            Thread.sleep(3000);
        }
    }
}