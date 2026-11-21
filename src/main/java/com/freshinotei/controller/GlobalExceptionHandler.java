package com.freshinotei.controller;

import com.freshinotei.service.GeminiVisionService.GeminiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 에러 응답 통일.
 *
 * 응답 본문은 Spring Boot 기본 에러 포맷(timestamp/status/error/path)을 그대로 유지하고
 * message 필드만 추가한다. 그 외 예외(404 등)는 Spring 기본 처리에 맡김 → 기존 프론트가 기본 포맷을 읽고 있어도 깨지지 않음.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> badRequest(Exception e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "이미지 파일(image)을 확인해 주세요. " + safe(e), req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(Exception e, HttpServletRequest req) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 10MB 이하만 업로드할 수 있습니다.", req);
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<Map<String, Object>> gemini(GeminiException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_GATEWAY, e.getMessage(), req);
    }

    private static String safe(Exception e) {
        return e instanceof IllegalArgumentException && e.getMessage() != null ? e.getMessage() : "";
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message,
                                                            HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(com.freshinotei.config.AppTime.ZONE).toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
