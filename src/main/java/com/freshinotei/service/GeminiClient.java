package com.freshinotei.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent 공통 호출.
 *
 * - API 키는 x-goog-api-key 헤더로 전송 (URL·로그에 키가 남지 않도록)
 * - response_mime_type=application/json 으로 JSON 응답 강제
 * - Gemini 3 계열은 temperature 기본값(1.0) 유지를 권장 → 지정하지 않음
 * - thought 파트 제외 후 text 파트만 이어붙이고, ```json 코드펜스가 있으면 제거
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final RestClient geminiRestClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            RestClient geminiRestClient,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-3.1-flash-lite}") String model
    ) {
        this.geminiRestClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * @param parts   요청 parts (text / inline_data 등)
     * @param purpose 로그용 설명 (예: "영수증 분석")
     * @return 모델이 반환한 JSON 문자열
     */
    public String generateJson(List<Map<String, Object>> parts, String purpose) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        try {
            JsonNode response = geminiRestClient.post()
                    .uri(String.format(GEMINI_URL, model))
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String text = extractText(response);
            log.info("[Gemini 응답 원본 - {}]\n{}", purpose, text);
            return text;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패 ({}, model={})", purpose, model, e);
            throw new GeminiException(purpose + "에 실패했습니다.", e);
        }
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Gemini 응답이 비어 있습니다.");
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) continue;
            if (part.has("text")) sb.append(part.get("text").asText());
        }
        if (sb.isEmpty()) {
            throw new IllegalStateException("Gemini 응답에 텍스트가 없습니다: "
                    + response.path("promptFeedback") + " / finishReason="
                    + response.path("candidates").path(0).path("finishReason").asText("?"));
        }
        return stripCodeFence(sb.toString().trim());
    }

    private static String stripCodeFence(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }
}
