package com.freshinotei.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshinotei.dto.PriorityIngredient;
import com.freshinotei.dto.Recipe;
import com.freshinotei.repository.IngredientStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 유통기한 임박 재료 우선 레시피 추천.
 *
 * 1) 보유 재료를 우선순위 순으로 정렬, 이미 만료(EXPIRED)된 재료는 제외
 * 2) 상위 MAX_INGREDIENTS 개를 Gemini 에 전달해 레시피 RECIPE_COUNT 개 요청
 * 3) 재료가 없으면 Gemini 를 호출하지 않고 빈 리스트 반환
 */
@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private static final int MAX_INGREDIENTS = 8;
    private static final int RECIPE_COUNT = 3;

    private static final String RECIPE_PROMPT = """
            당신은 1인 가구를 위한 가정식 요리 전문가입니다.
            아래는 사용자의 냉장고에 있는 식재료이며, 위에 있을수록 유통기한이 임박해 먼저 써야 합니다.

            [냉장고 식재료]
            %s

            [규칙]
            1. 레시피를 정확히 %d개 추천합니다.
            2. 목록의 위쪽(유통기한 임박) 재료를 최대한 활용합니다.
            3. 1인분 기준, 집에서 30분 안팎으로 만들 수 있는 쉬운 요리로 제안합니다.
            4. usedIngredients 에는 위 목록에 있는 재료 이름만 목록과 똑같이 적습니다.
            5. extraIngredients 에는 목록에 없지만 필요한 재료(양념, 기본 식재료)를 적습니다.
            6. reason 에는 어떤 임박 재료를 소진하는지 짧게 적습니다. (예: "삼겹살 D-1, 양파 D-3")
            7. steps 는 3~6단계, 각 단계는 한 문장으로 짧게 적습니다.
            8. 설명, 마크다운, 코드블록 없이 순수 JSON 만 반환합니다.

            [응답 스키마]
            {
              "recipes": [
                {
                  "title": "string",
                  "reason": "string",
                  "cookTimeMinutes": 15,
                  "usedIngredients": ["string"],
                  "extraIngredients": ["string"],
                  "steps": ["string"]
                }
              ]
            }
            """;

    private final IngredientStore store;
    private final PriorityCalculator priorityCalculator;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public RecipeService(IngredientStore store,
                         PriorityCalculator priorityCalculator,
                         GeminiClient geminiClient,
                         ObjectMapper objectMapper) {
        this.store = store;
        this.priorityCalculator = priorityCalculator;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public List<Recipe> recommend() {
        List<PriorityIngredient> candidates = store.findAll().stream()
                .map(priorityCalculator::calculate)
                .filter(p -> !"EXPIRED".equals(p.urgency()))
                .sorted(Comparator.comparingDouble(PriorityIngredient::priorityScore).reversed())
                .limit(MAX_INGREDIENTS)
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        String ingredientLines = candidates.stream()
                .map(p -> "- %s (%s, D-%d)".formatted(
                        p.ingredient().name(), p.ingredient().category(), p.daysLeft()))
                .collect(Collectors.joining("\n"));

        String rawJson = geminiClient.generateJson(
                List.of(Map.of("text", RECIPE_PROMPT.formatted(ingredientLines, RECIPE_COUNT))),
                "레시피 추천");

        try {
            return parseRecipes(rawJson);
        } catch (Exception e) {
            throw new GeminiException("레시피 추천에 실패했습니다.", e);
        }
    }

    private List<Recipe> parseRecipes(String rawJson) throws Exception {
        JsonNode arr = objectMapper.readTree(rawJson).path("recipes");
        List<Recipe> result = new ArrayList<>();
        for (JsonNode node : arr) {
            String title = node.path("title").asText("").trim();
            if (title.isEmpty()) {
                log.warn("레시피 파싱 스킵 (title 없음): {}", node);
                continue;
            }
            result.add(new Recipe(
                    title,
                    node.path("reason").asText(""),
                    node.path("cookTimeMinutes").asInt(0),
                    toStringList(node.path("usedIngredients")),
                    toStringList(node.path("extraIngredients")),
                    toStringList(node.path("steps"))
            ));
        }
        return result;
    }

    private static List<String> toStringList(JsonNode arr) {
        List<String> list = new ArrayList<>();
        for (JsonNode n : arr) {
            String v = n.asText("").trim();
            if (!v.isEmpty()) list.add(v);
        }
        return list;
    }
}
