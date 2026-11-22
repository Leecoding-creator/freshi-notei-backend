package com.freshinotei.dto;

import java.util.List;

public record ScanResponse(
        int count,
        List<ScannedIngredient> ingredients
) {}
