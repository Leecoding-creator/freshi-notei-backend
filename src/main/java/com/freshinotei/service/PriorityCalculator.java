package com.freshinotei.service;

import com.freshinotei.domain.Ingredient;
import com.freshinotei.dto.PriorityIngredient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 식재료의 "소비 우선순위" 산정.
 *
 *   priorityScore = base(daysLeft) × riskWeight(category)
 *
 * - daysLeft: 남은 유통기한이 짧을수록 base 점수 ↑
 * - riskWeight: 부패가 빠른 카테고리(수산물/육류)일수록 가중치 ↑
 * - 0~100 으로 클램프하여 정렬·UI 표시에 바로 쓸 수 있게 함
 */
@Component
public class PriorityCalculator {

    public PriorityIngredient calculate(Ingredient ingredient) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), ingredient.expirationDate());
        double riskWeight = riskWeightOf(ingredient.category());

        double base;
        String urgency;

        if (daysLeft < 0) {
            base = 100.0;          // 이미 만료
            urgency = "EXPIRED";
        } else if (daysLeft == 0) {
            base = 95.0;
            urgency = "TODAY";
        } else if (daysLeft <= 1) {
            base = 85.0;
            urgency = "URGENT";
        } else if (daysLeft <= 3) {
            base = 60.0;
            urgency = "SOON";
        } else if (daysLeft <= 7) {
            base = 30.0;
            urgency = "WATCH";
        } else {
            base = Math.max(5.0, 20.0 - daysLeft);
            urgency = "FRESH";
        }

        double score = Math.min(100.0, base * riskWeight);
        return new PriorityIngredient(ingredient, daysLeft, score, urgency);
    }

    private double riskWeightOf(String category) {
        if (category == null) return 1.0;
        return switch (category) {
            case "수산물", "육류" -> 1.5;
            case "유제품", "채소" -> 1.2;
            case "과일" -> 1.0;
            default -> 0.7;       // 곡류·조미료·기타
        };
    }
}
