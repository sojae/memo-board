#!/bin/bash
# ============================================================
# EC2 배포 스크립트
# JAR 교체 + 서버 재시작
# ============================================================

set -e

APP_DIR="$HOME/memo-board"
JAR_FILE="$APP_DIR/app.jar"
LOG_FILE="$APP_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"

echo ">>> 배포 시작"

# 기존 프로세스 종료
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo ">>> 기존 프로세스 종료 (PID: $OLD_PID)"
        kill "$OLD_PID"
        sleep 5
        # 아직 살아있으면 강제 종료
        if kill -0 "$OLD_PID" 2>/dev/null; then
            echo ">>> 강제 종료"
            kill -9 "$OLD_PID"
            sleep 2
        fi
    fi
    rm -f "$PID_FILE"
fi

# 애플리케이션 시작
echo ">>> 애플리케이션 시작"
nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    > "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"
echo ">>> 애플리케이션 시작됨 (PID: $NEW_PID)"

# 헬스 체크 (최대 60초 대기)
echo ">>> 헬스 체크 시작"
for i in $(seq 1 12); do
    sleep 5
    if curl -sf http://localhost:8080 > /dev/null 2>&1; then
        echo ">>> 서버 정상 가동 확인"
        echo ">>> 배포 완료"
        exit 0
    fi
    echo ">>> 대기 중... ($((i * 5))초)"
done

echo ">>> 서버 시작 실패 - 로그 확인 필요"
tail -50 "$LOG_FILE"
exit 1
