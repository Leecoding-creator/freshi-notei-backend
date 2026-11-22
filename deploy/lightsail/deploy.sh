#!/usr/bin/env bash
# FreshiNotei 백엔드 — 최신 main 받아서 빌드 후 재시작
#
# 사용법 (코드 업데이트할 때마다):
#   sudo bash /opt/freshinotei/deploy/lightsail/deploy.sh
set -euo pipefail

APP_DIR="/opt/freshinotei"
APP_USER="freshi"
BRANCH="${BRANCH:-main}"

if [[ $EUID -ne 0 ]]; then
  echo "sudo 로 실행해 주세요." >&2
  exit 1
fi

if grep -q '여기에_API_키_입력' /etc/freshinotei.env; then
  echo "/etc/freshinotei.env 에 GEMINI_API_KEY 를 먼저 입력해 주세요." >&2
  exit 1
fi

echo "==> 코드 업데이트 ($BRANCH)"
sudo -u "$APP_USER" git -C "$APP_DIR" fetch origin "$BRANCH"
sudo -u "$APP_USER" git -C "$APP_DIR" checkout -q "$BRANCH"
sudo -u "$APP_USER" git -C "$APP_DIR" reset --hard "origin/$BRANCH"

echo "==> 빌드"
cd "$APP_DIR"
sudo -u "$APP_USER" mvn -q -B clean package -DskipTests
sudo -u "$APP_USER" cp target/freshi-notei-backend-*.jar "$APP_DIR/app.jar"

echo "==> 재시작"
systemctl restart freshinotei

echo "==> 기동 대기"
for i in $(seq 1 60); do
  if curl -fs http://127.0.0.1:8080/ingredients >/dev/null; then
    echo "OK — $(git -C "$APP_DIR" log -1 --format='%h %s')"
    exit 0
  fi
  sleep 2
done

echo "120초 안에 응답이 없습니다. 로그 확인: journalctl -u freshinotei -n 100 --no-pager" >&2
exit 1
