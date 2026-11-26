#!/usr/bin/env bash
# FreshiNotei 백엔드 — Lightsail(Ubuntu 22.04/24.04) 최초 1회 설정
#
# 사용법:
#   sudo bash setup.sh freshinotei.duckdns.org
#
# 하는 일:
#   1) Java 17, Maven, Git, Caddy 설치
#   2) 스왑 1GB 생성 (서버에서 Maven 빌드할 때 메모리 여유용)
#   3) 실행 전용 사용자(freshi) 생성, 코드 clone → /opt/freshinotei
#   4) 환경변수 파일(/etc/freshinotei.env) 생성 — GEMINI_API_KEY 는 직접 입력
#   5) systemd 서비스 + Caddy(HTTPS 자동 발급) 등록
set -euo pipefail

DOMAIN="${1:-}"
REPO_URL="https://github.com/Leecoding-creator/freshi-notei-backend.git"
APP_DIR="/opt/freshinotei"
APP_USER="freshi"
ENV_FILE="/etc/freshinotei.env"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ $EUID -ne 0 ]]; then
  echo "sudo 로 실행해 주세요: sudo bash setup.sh <도메인>" >&2
  exit 1
fi
if [[ -z "$DOMAIN" ]]; then
  echo "도메인을 인자로 넣어 주세요. 예) sudo bash setup.sh freshinotei.duckdns.org" >&2
  exit 1
fi
if ! command -v apt-get >/dev/null; then
  echo "Ubuntu/Debian 전용 스크립트입니다. (Amazon Linux 는 별도 안내 필요)" >&2
  exit 1
fi

echo "==> [1/6] 패키지 설치 (Java 17, Maven, Git)"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y openjdk-17-jdk-headless maven git curl \
  debian-keyring debian-archive-keyring apt-transport-https gnupg

echo "==> [2/6] Caddy 설치 (공식 저장소)"
if ! command -v caddy >/dev/null; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
    | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
    > /etc/apt/sources.list.d/caddy-stable.list
  chmod o+r /usr/share/keyrings/caddy-stable-archive-keyring.gpg /etc/apt/sources.list.d/caddy-stable.list
  apt-get update -y
  apt-get install -y caddy
fi

echo "==> [3/6] 스왑 1GB"
if ! swapon --show | grep -q '/swapfile'; then
  fallocate -l 1G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> [4/6] 실행 사용자 / 코드 준비"
id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --create-home --shell /usr/sbin/nologin "$APP_USER"
if [[ ! -d "$APP_DIR/.git" ]]; then
  git clone "$REPO_URL" "$APP_DIR"
fi
chown -R "$APP_USER":"$APP_USER" "$APP_DIR"
# root 로 git 을 실행해도 "dubious ownership" 오류가 나지 않도록 예외 등록
git config --system --get-all safe.directory 2>/dev/null | grep -qx "$APP_DIR" \
  || git config --system --add safe.directory "$APP_DIR"

echo "==> [5/6] 환경변수 파일"
# 파일이 없거나, 비어 있거나(예: 설치 실패 중 nano 로 빈 파일 저장), GEMINI_API_KEY 항목이 없으면 템플릿으로 생성.
# 이미 키가 들어 있는 파일은 건드리지 않음.
if [[ ! -s "$ENV_FILE" ]] || ! grep -q '^GEMINI_API_KEY=' "$ENV_FILE"; then
  if [[ -f "$ENV_FILE" ]]; then
    cp "$ENV_FILE" "$ENV_FILE.bak.$(date +%s)"
  fi
  install -m 600 -o root -g root "$SCRIPT_DIR/freshinotei.env.example" "$ENV_FILE"
  echo "    $ENV_FILE 생성 (템플릿)"
else
  chmod 600 "$ENV_FILE"
  echo "    $ENV_FILE 이미 있음 — 유지"
fi

echo "==> [6/6] systemd 서비스 + Caddy 설정"
install -m 644 "$SCRIPT_DIR/freshinotei.service" /etc/systemd/system/freshinotei.service
sed "s/__DOMAIN__/$DOMAIN/" "$SCRIPT_DIR/Caddyfile" > /etc/caddy/Caddyfile
systemctl daemon-reload
systemctl enable freshinotei
systemctl reload caddy || systemctl restart caddy

echo
echo "설정 완료. 다음 순서로 진행하세요:"
echo "  1) sudo nano $ENV_FILE   → GEMINI_API_KEY 입력"
echo "  2) sudo bash $APP_DIR/deploy/lightsail/deploy.sh   → 빌드 + 실행"
echo "  3) 브라우저에서 https://$DOMAIN/ingredients  → [] 가 보이면 성공"
