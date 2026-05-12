package com.freshinotei.dto;

import com.freshinotei.domain.Ingredient;

import java.util.List;

public record ScanResponse(
        int count,
        List<Ingredient> ingredients
) {}
