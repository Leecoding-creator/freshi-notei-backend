package com.freshinotei.dto;

import com.freshinotei.domain.Ingredient;

/**
 * 소비 우선순위 결과.
 *
 * @param ingredient    원 식재료
 * @param daysLeft      유통기한까지 남은 일수 (음수면 만료)
 * @param priorityScore 0~100, 높을수록 빨리 먹어야 함
 * @param urgency       EXPIRED / TODAY / URGENT / SOON / WATCH / FRESH
 */
public record PriorityIngredient(
        Ingredient ingredient,
        long daysLeft,
        double priorityScore,
        String urgency
) {}
