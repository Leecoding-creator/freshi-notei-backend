package com.freshinotei.controller;

import com.freshinotei.domain.Ingredient;
import com.freshinotei.dto.PriorityIngredient;
import com.freshinotei.dto.ScanResponse;
import com.freshinotei.dto.ScannedIngredient;
import com.freshinotei.repository.IngredientStore;
import com.freshinotei.service.GeminiVisionService;
import com.freshinotei.service.PriorityCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private static final Logger log = LoggerFactory.getLogger(IngredientController.class);

    private final GeminiVisionService geminiService;
    private final IngredientStore store;
    private final PriorityCalculator priorityCalculator;

    public IngredientController(GeminiVisionService geminiService,
                                IngredientStore store,
                                PriorityCalculator priorityCalculator) {
        this.geminiService = geminiService;
        this.store = store;
        this.priorityCalculator = priorityCalculator;
    }

    /**
     * 영수증 이미지 업로드 → Gemini 로 식재료 추출 → 메모리 저장.
     *
     * curl 예시:
     *   curl -X POST http://localhost:8080/ingredients/scan \
     *        -F "image=@receipt.jpg"
     */
    @Operation(summary = "영수증 이미지 스캔", description = "영수증 이미지를 업로드하면 Gemini로 식재료를 추출하여 저장합니다. 각 항목에 daysLeft/priorityScore/urgency 포함.")
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScanResponse scan(
            @Parameter(
                    description = "영수증 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("image") MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 비어있습니다.");
        }

        log.info("영수증 수신: {} ({} bytes, {})",
                image.getOriginalFilename(), image.getSize(), image.getContentType());

        List<Ingredient> ingredients = geminiService.extractFromReceipt(
                image.getBytes(), image.getContentType());

        store.saveAll(ingredients);

        log.info("추출 완료: {}건", ingredients.size());
        ingredients.forEach(i -> log.debug("  └ {}", i));

        List<ScannedIngredient> result = ingredients.stream()
                .map(priorityCalculator::calculate)
                .map(ScannedIngredient::of)
                .toList();
        return new ScanResponse(result.size(), result);
    }

    /**
     * 현재 보유 중인 식재료를 소비 우선순위 내림차순으로 반환.
     */
    @GetMapping("/priority")
    public List<PriorityIngredient> priority() {
        return store.findAll().stream()
                .map(priorityCalculator::calculate)
                .sorted(Comparator.comparingDouble(PriorityIngredient::priorityScore).reversed())
                .toList();
    }

    /**
     * 데모용 전체 조회.
     */
    @GetMapping
    public List<Ingredient> all() {
        return store.findAll();
    }

    /**
     * 식재료 1개 삭제 (먹었음 / 버림).
     */
    @Operation(summary = "식재료 개별 삭제", description = "id 는 /ingredients/priority 응답의 ingredient.id. 성공 204, 없는 id 면 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제됨"),
            @ApiResponse(responseCode = "404", description = "해당 id 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return store.remove(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * 데모 리셋.
     */
    @DeleteMapping
    public void clear() {
        store.clear();
    }
}
