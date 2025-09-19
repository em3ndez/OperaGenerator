package com.kousenit.util;

import java.util.concurrent.Semaphore;

/**
 * AutoCloseable wrapper for Semaphore to enable try-with-resources pattern.
 * Automatically releases the permit when closed.
 */
public class SemaphorePermit implements AutoCloseable {
    private final Semaphore semaphore;
    private boolean released = false;

    /**
     * Acquires a permit from the semaphore.
     *
     * @param semaphore the semaphore to acquire from
     * @throws InterruptedException if interrupted while acquiring
     */
    public SemaphorePermit(Semaphore semaphore) throws InterruptedException {
        this.semaphore = semaphore;
        semaphore.acquire();
    }

    /**
     * Releases the permit back to the semaphore.
     * Safe to call multiple times - subsequent calls are no-ops.
     */
    @Override
    public void close() {
        if (!released) {
            semaphore.release();
            released = true;
        }
    }
}