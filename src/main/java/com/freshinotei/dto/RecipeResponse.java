package com.freshinotei.dto;

import java.util.List;

public record RecipeResponse(
        List<Recipe> recipes
) {}
