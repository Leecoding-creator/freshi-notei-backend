package com.freshinotei.controller;

import com.freshinotei.dto.RecipeResponse;
import com.freshinotei.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Operation(
            summary = "레시피 추천",
            description = "유통기한이 임박한 재료를 우선 활용하는 레시피 3개를 추천합니다. "
                    + "요청 본문 없음. 만료된 재료는 제외하며, 냉장고가 비어 있으면 recipes 가 빈 배열입니다. "
                    + "AI 호출이라 응답에 수 초~10초 정도 걸릴 수 있습니다."
    )
    @PostMapping("/recommend")
    public RecipeResponse recommend() {
        return new RecipeResponse(recipeService.recommend());
    }
}
