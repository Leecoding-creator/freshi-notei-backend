# FreshiNotei Backend API 명세서

> 프론트엔드(React/Next.js) 연동용 문서
> 최종 업데이트: 2026-11-22

## Base URL

| 환경 | URL |
|---|---|
| 로컬 개발 | `http://localhost:8080` |
| 배포 | `https://freshinotei.duckdns.org` |

## Swagger UI

브라우저에서 다음 주소로 접속하면 API 를 직접 테스트할 수 있습니다.

```
https://freshinotei.duckdns.org/swagger-ui/index.html
```

---

## 1. 영수증 스캔 (식재료 자동 등록)

영수증 이미지를 업로드하면 AI(Gemini Vision)가 식재료 이름, 카테고리, 유통기한을 추출하여 자동 등록합니다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/ingredients/scan` |
| Content-Type | `multipart/form-data` |
| 파라미터명 | `image` (필수, 이미지 파일) |
| 지원 포맷 | JPG, PNG, WebP |
| 최대 크기 | 10MB |
| 응답 시간 | 5~15초 (Gemini API 분석 시간) |

### 요청 예시 (React/Next.js)

```javascript
const formData = new FormData();
formData.append('image', file);  // file 은 input type="file" 의 File 객체

const response = await fetch('http://localhost:8080/ingredients/scan', {
  method: 'POST',
  body: formData,
});

const data = await response.json();
```

### 응답 예시

```json
{
  "count": 6,
  "ingredients": [
    {
      "id": "05680605-fb00-4410-8dda-a56937940b83",
      "name": "깻잎",
      "purchaseDate": "2026-05-12",
      "expirationDate": "2026-05-17",
      "category": "채소",
      "daysLeft": 5,
      "priorityScore": 36.0,
      "urgency": "WATCH"
    }
  ]
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `count` | int | 추출된 식재료 개수 |
| `ingredients[].id` | string (UUID) | 식재료 고유 ID |
| `ingredients[].name` | string | 식재료 이름 |
| `ingredients[].purchaseDate` | string (YYYY-MM-DD) | 구매일 |
| `ingredients[].expirationDate` | string (YYYY-MM-DD) | 유통기한 |
| `ingredients[].category` | string | 카테고리 (채소/과일/육류/수산물/유제품/곡류/기타) |
| `ingredients[].daysLeft` | int | 유통기한까지 남은 일수 (2번 우선순위 조회와 같은 계산) |
| `ingredients[].priorityScore` | double | 우선순위 점수 (0~100) |
| `ingredients[].urgency` | string | EXPIRED / TODAY / URGENT / SOON / WATCH / FRESH |

---

## 2. 소비 우선순위 조회

현재 보유한 모든 식재료를 **소비해야 할 우선순위 순**으로 정렬해서 반환합니다.

알고리즘:
- D-day 가 짧을수록 점수 ↑
- 부패 위험 카테고리(수산물 1.5, 육류 1.5, 유제품 1.2, 채소 1.2, 과일 1.0, 기타 0.7) 가중치 곱
- 0~100 점수

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/ingredients/priority` |

### 요청 예시

```javascript
const response = await fetch('http://localhost:8080/ingredients/priority');
const priorityList = await response.json();
```

### 응답 예시

```json
[
  {
    "ingredient": {
      "id": "c9fdbf57-5744-45db-90ba-092c4ec81d35",
      "name": "양상추",
      "purchaseDate": "2026-05-12",
      "expirationDate": "2026-05-17",
      "category": "채소"
    },
    "daysLeft": 5,
    "priorityScore": 36.0,
    "urgency": "WATCH"
  }
]
```

### 응답 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `ingredient` | object | 식재료 정보 (위 1번과 동일) |
| `daysLeft` | int | 유통기한까지 남은 일수 (음수면 만료) |
| `priorityScore` | double | 우선순위 점수 (0~100, 높을수록 빨리 먹어야 함) |
| `urgency` | string | EXPIRED / TODAY / URGENT / SOON / WATCH / FRESH |

### urgency 값별 의미

| 값 | 의미 | 권장 UI 색상 |
|---|---|---|
| `EXPIRED` | 유통기한 초과 | 회색 (소비 불가) |
| `TODAY` | 오늘까지 | 빨강 |
| `URGENT` | 1일 이내 | 빨강 |
| `SOON` | 3일 이내 | 주황 |
| `WATCH` | 7일 이내 | 노랑 |
| `FRESH` | 7일 초과 | 초록 |

---

## 3. 전체 식재료 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/ingredients` |

### 응답 예시

```json
[
  {
    "id": "05680605-...",
    "name": "깻잎",
    "purchaseDate": "2026-05-12",
    "expirationDate": "2026-05-17",
    "category": "채소"
  }
]
```

---

## 4. 전체 초기화 (데모용)

| 항목 | 값 |
|---|---|
| Method | `DELETE` |
| Path | `/ingredients` |

데모 시연 후 데이터 초기화 시 사용.

---

## 5. 식재료 개별 삭제

먹었거나 버린 재료 1개를 삭제합니다.

| 항목 | 값 |
|---|---|
| Method | `DELETE` |
| Path | `/ingredients/{id}` |
| id | `/ingredients/priority` 응답의 `ingredient.id` |
| 응답 | 성공 `204` (본문 없음), 없는 id 면 `404` |

```javascript
await fetch(`${BASE_URL}/ingredients/${id}`, { method: 'DELETE' });
// 이후 /ingredients/priority 다시 조회해서 화면 갱신
```

---

## 6. 레시피 추천

유통기한이 임박한 재료를 우선 활용하는 레시피 3개를 AI 가 추천합니다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/recipes/recommend` |
| 요청 본문 | 없음 |
| 응답 시간 | 수 초~10초 (Gemini 호출) → 로딩 화면 필요 |

- 우선순위 상위 8개 재료를 사용하며, 이미 만료된(EXPIRED) 재료는 제외합니다.
- 냉장고가 비어 있으면 `recipes` 가 빈 배열입니다.

```javascript
const res = await fetch(`${BASE_URL}/recipes/recommend`, { method: 'POST' });
const { recipes } = await res.json();
```

### 응답 예시

```json
{
  "recipes": [
    {
      "title": "삼겹살 김치볶음",
      "reason": "삼겹살 D-1, 양파 D-3",
      "cookTimeMinutes": 15,
      "usedIngredients": ["삼겹살", "양파"],
      "extraIngredients": ["김치", "간장"],
      "steps": ["삼겹살을 굽는다", "양파와 김치를 넣고 볶는다", "간장으로 간을 맞춘다"]
    }
  ]
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `title` | string | 요리 이름 |
| `reason` | string | 추천 이유 (소진하는 임박 재료) |
| `cookTimeMinutes` | int | 예상 조리 시간(분) |
| `usedIngredients` | string[] | 냉장고에 있는 재료 중 사용하는 것 |
| `extraIngredients` | string[] | 추가로 필요한 재료 (양념 등) |
| `steps` | string[] | 조리 순서 |

---

## CORS

전체 도메인 허용. 프론트엔드는 별도 설정 없이 호출 가능합니다.

## 에러 처리

| HTTP 상태 | 의미 |
|---|---|
| 200 / 204 | 성공 |
| 400 | 잘못된 요청 (이미지 누락·빈 파일) |
| 404 | 없는 식재료 id (개별 삭제) |
| 413 | 이미지 10MB 초과 |
| 502 | AI(Gemini) 호출·분석 실패 |

에러 응답 형식: `{ "timestamp", "status", "error", "message", "path" }`
사용자 안내 문구는 `message` 를 쓰면 됩니다.

---

## 데모 시나리오 (UI 흐름 제안)

1. **영수증 촬영/업로드 화면**
   - 사용자가 영수증 사진 업로드
   - → `POST /ingredients/scan` 호출
   - 로딩 중: "AI가 영수증을 분석하고 있어요" (디자인 시안의 분석 화면 활용)

2. **추출 결과 확인 화면**
   - 응답 받은 식재료 리스트 표시
   - "냉장고에 자동 저장하기" 버튼 (디자인 시안 그대로)

3. **내 냉장고 화면**
   - → `GET /ingredients/priority` 호출
   - urgency 값에 따라 색상 다르게 표시 (D-1 빨강, D-2 주황 등)
   - 디자인 시안의 "총 12개의 식재료가 있어요" 화면 그대로
