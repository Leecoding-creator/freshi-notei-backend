# 배포 가이드

현재 배포: **AWS Lightsail (Ubuntu, 2GB) + Caddy(HTTPS) + DuckDNS 무료 도메인**
(Railway 무료 기간 종료로 이전. 예전 Railway 가이드는 문서 아래쪽에 남겨둠)

## Lightsail 배포

### 0. 준비물
- Lightsail Ubuntu 22.04/24.04 인스턴스
- Gemini API 키

### 1. 고정 IP 연결
Lightsail 콘솔 → **Networking → Create static IP** → 인스턴스에 연결.
(고정 IP 없으면 재시작할 때 IP 가 바뀌어 도메인이 끊김)

### 2. 방화벽
인스턴스 → **Networking → IPv4 Firewall** 에서 아래만 열어둠.

| 포트 | 용도 |
|---|---|
| 22 | SSH |
| 80 | HTTPS 인증서 발급·리다이렉트 |
| 443 | HTTPS |

8080 은 열지 않음 (Spring 은 localhost 에만 바인딩, Caddy 가 앞에서 받음).

### 3. DuckDNS 도메인
1. https://www.duckdns.org 로그인 → 서브도메인 생성 (예: `freshinotei`)
2. **current ip** 에 1번 고정 IP 입력 → update ip

### 4. 서버 설정 (최초 1회)
```bash
git clone https://github.com/Leecoding-creator/freshi-notei-backend.git ~/freshi-setup
sudo bash ~/freshi-setup/deploy/lightsail/setup.sh freshinotei.duckdns.org
sudo nano /etc/freshinotei.env        # GEMINI_API_KEY 입력
sudo bash /opt/freshinotei/deploy/lightsail/deploy.sh
```

`https://freshinotei.duckdns.org/ingredients` 에서 `[]` 가 보이면 성공.
Swagger: `https://freshinotei.duckdns.org/swagger-ui/index.html`

### 5. 코드 업데이트 (자동 배포)
main 에 머지하면 GitHub Actions(`.github/workflows/deploy.yml`)가 서버에 SSH 로 접속해 `deploy.sh` 를 실행합니다.
`.md` 문서만 바뀐 경우는 배포하지 않습니다. Actions 탭 → **Deploy to Lightsail → Run workflow** 로 수동 실행도 가능.

최초 1회 설정: GitHub 저장소 **Settings → Secrets and variables → Actions → New repository secret**
- Name: `LIGHTSAIL_SSH_KEY`
- Value: Lightsail 기본 SSH 키(`LightsailDefaultKey-ap-northeast-2.pem`) 파일 내용 전체 (`-----BEGIN` ~ `END-----` 포함)

수동 배포가 필요하면 서버에서:
```bash
sudo bash /opt/freshinotei/deploy/lightsail/deploy.sh
```

### 자주 쓰는 명령
```bash
journalctl -u freshinotei -f             # 백엔드 로그
sudo systemctl restart freshinotei       # 재시작 (환경변수 바꾼 뒤)
journalctl -u caddy -n 50 --no-pager     # HTTPS 인증서 문제 확인
```

### 트러블슈팅
- **HTTPS 접속 안 됨**: 방화벽 80/443, DuckDNS IP 가 고정 IP 와 같은지, `journalctl -u caddy` 확인
- **502 Bad Gateway**: 백엔드가 안 떠 있음 → `journalctl -u freshinotei -n 100`
- **스캔만 실패(502 + "영수증 분석에 실패했습니다")**: `/etc/freshinotei.env` 의 API 키·모델명 확인

---

# (이전) Railway 배포 가이드

가장 쉽고 무료인 Railway 로 배포합니다.

## 사전 준비

- GitHub 계정 (없으면 만들기)
- Railway 계정 (GitHub 로 가입 가능)

## Step 1. GitHub 에 코드 올리기

### 1-1. GitHub 에서 새 저장소 만들기

1. https://github.com/new 접속
2. **Repository name**: `freshi-noti-backend`
3. **Public** 선택 (Private 도 가능)
4. **Create repository** 클릭

### 1-2. 로컬에서 Git 초기화 후 푸시

프로젝트 폴더에서 PowerShell 열고 다음 명령어 순서대로 실행:

```powershell
cd C:\Users\User\Desktop\아이디어톤_백엔드\files\freshi-noti-backend

# Git 초기화
git init
git branch -M main

# .gitignore 생성 (target 폴더, IDE 설정 제외)
@"
target/
.idea/
*.iml
.DS_Store
"@ | Out-File -Encoding UTF8 .gitignore

# 커밋
git add .
git commit -m "Initial commit: FreshiNotei MVP"

# 본인의 GitHub 저장소 URL 로 변경하세요
git remote add origin https://github.com/본인계정/freshi-noti-backend.git
git push -u origin main
```

> 💡 `git` 명령어가 없다는 에러가 나면 https://git-scm.com/download/win 에서 Git 설치 후 PowerShell 재시작.

## Step 2. Railway 배포

### 2-1. 회원가입

1. https://railway.app 접속
2. **Login with GitHub** 클릭

### 2-2. 프로젝트 생성

1. 대시보드에서 **New Project** 클릭
2. **Deploy from GitHub repo** 선택
3. 권한 요청 시 GitHub 접근 허용
4. 방금 만든 `freshi-noti-backend` 저장소 선택

### 2-3. 환경변수 추가 (중요!)

배포가 시작되면 좌측에 프로젝트가 보입니다.

1. 프로젝트 클릭 → **Variables** 탭 클릭
2. **New Variable** 클릭
3. 다음 두 개 추가:

| Name | Value |
|---|---|
| `GEMINI_API_KEY` | 발급받으신 Gemini API 키 |
| `JAVA_TOOL_OPTIONS` | `-Xmx512m` |
| `GEMINI_MODEL` (선택) | 기본값 `gemini-3.1-flash-lite` |

> 💡 `JAVA_TOOL_OPTIONS` 는 무료 플랜의 메모리 한계(512MB) 초과 방지용

### 2-4. 공개 URL 생성

1. **Settings** 탭 클릭
2. **Networking** 섹션의 **Generate Domain** 클릭
3. 자동 생성된 URL 복사 (예: `freshi-noti-backend-production.up.railway.app`)

## Step 3. 배포 확인

생성된 URL 뒤에 `/ingredients` 붙여서 브라우저로 접속:

```
https://여러분의-도메인.up.railway.app/ingredients
```

`[]` 가 보이면 ✅ 배포 성공.

Swagger UI 확인:
```
https://여러분의-도메인.up.railway.app/swagger-ui/index.html
```

## Step 4. 프론트팀에 전달

다음 정보를 프론트 담당자에게 공유하시면 됩니다:

```
🌐 백엔드 URL: https://여러분의-도메인.up.railway.app
📖 API 문서: API_GUIDE.md 파일 참고
🧪 Swagger 테스트: https://여러분의-도메인.up.railway.app/swagger-ui/index.html
```

## 트러블슈팅

### 빌드 실패
- Railway 로그 확인: 프로젝트 → **Deployments** → 최신 빌드 클릭
- Maven 빌드 에러는 보통 의존성 문제. 다시 push 시도.

### "Application failed to respond"
- 환경변수 `GEMINI_API_KEY` 가 빠졌을 확률 99%. **Variables** 탭 확인.

### 무료 한도
- Railway 무료 플랜: 월 $5 크레딧 (대략 500시간 실행 가능)
- 데모 시연 1회 + 심사 기간 띄워두기에 충분
- 한도 가까워지면 **Settings → Sleep** 활성화로 절약 가능

## 대안: Render 사용

Railway 가 어려우면 https://render.com 도 동일한 방식으로 가능합니다.
- **New → Web Service** → GitHub 저장소 연결
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/freshi-notei-backend-0.1.0.jar`
- 환경변수에 `GEMINI_API_KEY` 추가
