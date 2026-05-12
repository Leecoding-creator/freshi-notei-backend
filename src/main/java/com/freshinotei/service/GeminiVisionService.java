package com.freshinotei.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshinotei.domain.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini Vision API 로 영수증 이미지에서 식재료를 추출.
 *
 * 흐름:
 *   1) 이미지 → Base64
 *   2) 프롬프트 + inline_data 로 요청 본문 구성
 *   3) responseMimeType=application/json 으로 강제 → 깔끔한 JSON 회수
 *   4) candidates[0].content.parts[0].text 를 다시 파싱하여 도메인 객체로 변환
 */
@Service
public class GeminiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * 영수증 → 식재료 추출 프롬프트.
     * 핵심 포인트:
     *  - "JSON 외 텍스트 금지" 명시
     *  - 카테고리 enum 고정으로 후처리 단순화
     *  - 유통기한 미기재 시 카테고리별 보존기간 휴리스틱 적용
     *  - 오늘 날짜를 주입해 LLM 의 시간감각 부족을 보완
     */
    private static final String EXTRACTION_PROMPT = """
            당신은 영수증을 분석하여 식재료를 정리하는 전문가입니다.
            첨부된 영수증 이미지에서 '식재료'에 해당하는 품목만 추출하여
            아래 JSON 스키마에 정확히 맞춰 응답하세요.

            [규칙]
            1. 식재료가 아닌 항목(생활용품, 의류, 즉석조리식품 등)은 제외합니다.
            2. 영수증의 구매일이 보이면 purchaseDate 에 ISO 8601 (YYYY-MM-DD) 형식으로 기재하고,
               보이지 않으면 오늘 날짜(%s) 를 사용합니다.
            3. 유통기한이 인쇄되어 있지 않은 경우, 일반적인 식재료 보존 지식을 토대로
               expirationDate 를 합리적으로 추정합니다.
                 - 잎채소: 구매일 + 5일
                 - 뿌리채소: 구매일 + 14일
                 - 과일: 구매일 + 7일
                 - 생육/생선: 구매일 + 3일
                 - 유제품: 구매일 + 14일
                 - 계란: 구매일 + 21일
                 - 곡류/조미료/통조림: 구매일 + 90일
            4. category 는 다음 중 하나입니다:
               "채소" / "과일" / "육류" / "수산물" / "유제품" / "곡류" / "기타"
            5. 설명, 마크다운, 코드블록 없이 순수 JSON 만 반환합니다.

            [응답 스키마]
            {
              "ingredients": [
                {
                  "name": "string",
                  "purchaseDate": "YYYY-MM-DD",
                  "expirationDate": "YYYY-MM-DD",
                  "category": "string"
                }
              ]
            }
            """;

    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiVisionService(
            RestClient geminiRestClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model
    ) {
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<Ingredient> extractFromReceipt(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String resolvedMime = (mimeType == null || mimeType.isBlank()) ? "image/jpeg" : mimeType;
        String prompt = EXTRACTION_PROMPT.formatted(LocalDate.now());

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", resolvedMime,
                                        "data", base64
                                ))
                        )
                )),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "temperature", 0.2
                )
        );

        String url = String.format(GEMINI_URL, model, apiKey);

        try {
            JsonNode response = geminiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String rawJson = response
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            log.info("[Gemini 응답 원본]\n{}", rawJson);

            return parseIngredients(rawJson);

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new RuntimeException("영수증 분석에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private List<Ingredient> parseIngredients(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode arr = root.path("ingredients");

        List<Ingredient> result = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                result.add(Ingredient.newOf(
                        node.path("name").asText(),
                        LocalDate.parse(node.path("purchaseDate").asText()),
                        LocalDate.parse(node.path("expirationDate").asText()),
                        node.path("category").asText("기타")
                ));
            } catch (Exception e) {
                // 한 항목 파싱 실패가 전체를 막지 않도록 스킵
                log.warn("식재료 파싱 스킵: {} ({})", node, e.getMessage());
            }
        }
        return result;
    }
}
