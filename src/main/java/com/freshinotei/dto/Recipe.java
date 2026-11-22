package com.freshinotei.dto;

import java.util.List;

/**
 * 추천 레시피 1개.
 *
 * @param title            요리 이름
 * @param reason           추천 이유 (예: "삼겹살 D-1, 양파 D-3")
 * @param cookTimeMinutes  예상 조리 시간(분)
 * @param usedIngredients  냉장고에 있는 재료 중 사용하는 것
 * @param extraIngredients 추가로 필요한 재료 (양념 등)
 * @param steps            조리 순서
 */
public record Recipe(
        String title,
        String reason,
        int cookTimeMinutes,
        List<String> usedIngredients,
        List<String> extraIngredients,
        List<String> steps
) {}
