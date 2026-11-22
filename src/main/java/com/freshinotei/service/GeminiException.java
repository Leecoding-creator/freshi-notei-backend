package com.freshinotei.service;

/**
 * Gemini 호출·응답 파싱 실패. GlobalExceptionHandler 에서 502 로 매핑된다.
 */
public class GeminiException extends RuntimeException {

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
