# FreshiNotei Backend (MVP)

아이디어톤 가산점용 **영수증 → 식재료 자동 등록** 핵심 기능 데모.

## 스택
- Java 17 / Spring Boot 3.3
- Google Gemini Vision API (`gemini-2.5-flash`)
- 인메모리 저장소 (DB 미연결, 추후 PostgreSQL + JPA 전환 예정)

## 실행

```bash
export GEMINI_API_KEY=your_key_here
./mvnw spring-boot:run
```

## API

| Method | Path                  | 설명                               |
|--------|-----------------------|------------------------------------|
| POST   | `/ingredients/scan`   | 영수증 이미지 업로드 → 식재료 추출 |
| GET    | `/ingredients/priority` | 소비 우선순위 내림차순 리스트       |
| GET    | `/ingredients`        | 보유 식재료 전체                   |
| DELETE | `/ingredients`        | 데모 리셋                          |

## 데모 시나리오

```bash
# 1) 영수증 업로드
curl -X POST http://localhost:8080/ingredients/scan \
     -F "image=@sample-receipt.jpg"

# 2) 소비 우선순위 확인
curl http://localhost:8080/ingredients/priority | jq
```

응답 예:
```json
[
  {
    "ingredient": {
      "id": "8f9...",
      "name": "삼겹살",
      "purchaseDate": "2026-05-10",
      "expirationDate": "2026-05-13",
      "category": "육류"
    },
    "daysLeft": 3,
    "priorityScore": 90.0,
    "urgency": "SOON"
  }
]
```

## 추후 확장
- PostgreSQL + JPA 전환 (`IngredientStore` → `JpaRepository`)
- `/recipes/recommend` 엔드포인트 (보유 식재료 기반 Gemini 레시피 추천)
- 사용자/알림 도메인 추가
- 인증 (JWT)
