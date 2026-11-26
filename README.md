# FreshiNotei Backend (MVP)

아이디어톤 가산점용 **영수증 → 식재료 자동 등록** 핵심 기능 데모.

## 스택
- Java 17 / Spring Boot 3.3
- Google Gemini API (`gemini-3.1-flash-lite`, 환경변수 `GEMINI_MODEL` 로 교체 가능)
- 인메모리 저장소 (DB 미연결, 추후 PostgreSQL + JPA 전환 예정)

## 실행

```bash
cp .env.example .env          # .env 에 GEMINI_API_KEY 입력 (.env 는 커밋되지 않음)
set -a; source .env; set +a
mvn spring-boot:run
```

환경변수 목록은 `.env.example` 참고. 서버(Lightsail)용 템플릿은 `deploy/lightsail/freshinotei.env.example`.

## API

| Method | Path                  | 설명                               |
|--------|-----------------------|------------------------------------|
| POST   | `/ingredients/scan`   | 영수증 이미지 업로드 → 식재료 추출 |
| GET    | `/ingredients/priority` | 소비 우선순위 내림차순 리스트       |
| GET    | `/ingredients`        | 보유 식재료 전체                   |
| DELETE | `/ingredients`        | 데모 리셋                          |
| DELETE | `/ingredients/{id}`   | 식재료 개별 삭제 (204 / 404)       |
| POST   | `/recipes/recommend`  | 임박 재료 우선 레시피 3개 추천     |

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

## 에러 응답

Spring Boot 기본 포맷 + `message` 필드.

| 상황 | Status |
|---|---|
| `image` 누락 / 빈 파일 | 400 |
| 10MB 초과 | 413 |
| Gemini 호출·파싱 실패 | 502 |

```json
{ "timestamp": "...", "status": 502, "error": "Bad Gateway", "message": "영수증 분석에 실패했습니다.", "path": "/ingredients/scan" }
```

## 추후 확장
- PostgreSQL + JPA 전환 (`IngredientStore` → `JpaRepository`)
- 사용자/알림 도메인 추가
- 인증 (JWT)
