package com.kousenit.exception;

/**
 * Exception thrown when image generation fails.
 */
public class ImageGenerationException extends OperaGenerationException {

    private final int sceneNumber;

    public ImageGenerationException(String message, int sceneNumber) {
        super(message);
        this.sceneNumber = sceneNumber;
    }

    public ImageGenerationException(String message, int sceneNumber, Throwable cause) {
        super(message, cause);
        this.sceneNumber = sceneNumber;
    }

    public int getSceneNumber() {
        return sceneNumber;
    }
}