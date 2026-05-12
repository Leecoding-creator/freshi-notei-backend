# FreshiNotei Backend API 명세서

> 프론트엔드(React/Next.js) 연동용 문서
> 최종 업데이트: 2026-05-12

## Base URL

| 환경 | URL |
|---|---|
| 로컬 개발 | `http://localhost:8080` |
| 배포 | (배포 완료 후 추가 예정) |

## Swagger UI

브라우저에서 다음 주소로 접속하면 API 를 직접 테스트할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
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
      "category": "채소"
    },
    {
      "id": "fb67ba56-d532-407b-bdd8-dd532a851532",
      "name": "토마토",
      "purchaseDate": "2026-05-12",
      "expirationDate": "2026-05-19",
      "category": "과일"
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

## CORS

전체 도메인 허용. 프론트엔드는 별도 설정 없이 호출 가능합니다.

## 에러 처리

| HTTP 상태 | 의미 |
|---|---|
| 200 | 성공 |
| 400 | 잘못된 요청 (이미지 누락 등) |
| 500 | 서버 에러 (Gemini API 호출 실패 등) |

에러 응답 본문에는 메시지가 포함됩니다. 프론트에서 try-catch 로 감싸서 사용자에게 에러 메시지를 표시하면 됩니다.

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
