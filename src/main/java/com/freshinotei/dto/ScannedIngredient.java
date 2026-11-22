package com.freshinotei.dto;

import com.freshinotei.domain.Ingredient;

import java.time.LocalDate;

/**
 * 스캔 결과 항목.
 *
 * 기존 Ingredient 필드(id/name/purchaseDate/expirationDate/category)를 그대로 펼치고
 * 우선순위 정보(daysLeft/priorityScore/urgency)를 덧붙인다.
 * → 기존 프론트는 깨지지 않고, 결과 화면 D-day 를 냉장고 화면과 같은 값으로 표시할 수 있다.
 */
public record ScannedIngredient(
        String id,
        String name,
        LocalDate purchaseDate,
        LocalDate expirationDate,
        String category,
        long daysLeft,
        double priorityScore,
        String urgency
) {
    public static ScannedIngredient of(PriorityIngredient p) {
        Ingredient i = p.ingredient();
        return new ScannedIngredient(
                i.id(), i.name(), i.purchaseDate(), i.expirationDate(), i.category(),
                p.daysLeft(), p.priorityScore(), p.urgency()
        );
    }
}
