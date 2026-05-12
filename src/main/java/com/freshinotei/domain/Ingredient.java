package com.freshinotei.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 식재료 한 항목.
 * 데모용이라 record 로 단순하게.
 */
public record Ingredient(
        String id,
        String name,
        LocalDate purchaseDate,
        LocalDate expirationDate,
        String category
) {
    public static Ingredient newOf(String name, LocalDate purchaseDate,
                                   LocalDate expirationDate, String category) {
        return new Ingredient(
                UUID.randomUUID().toString(),
                name,
                purchaseDate,
                expirationDate,
                category
        );
    }
}
