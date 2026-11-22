package com.freshinotei.repository;

import com.freshinotei.domain.Ingredient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 데모용 인메모리 저장소.
 * 추후 PostgreSQL 기반 JPA Repository 로 교체될 자리를 잡아둠.
 */
@Repository
public class IngredientStore {

    private final Map<String, Ingredient> store = new ConcurrentHashMap<>();

    public void save(Ingredient ingredient) {
        store.put(ingredient.id(), ingredient);
    }

    public void saveAll(List<Ingredient> ingredients) {
        ingredients.forEach(this::save);
    }

    public List<Ingredient> findAll() {
        return new ArrayList<>(store.values());
    }

    /** @return 삭제됐으면 true, 없는 id 면 false */
    public boolean remove(String id) {
        return store.remove(id) != null;
    }

    public void clear() {
        store.clear();
    }
}
