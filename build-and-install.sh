#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") [debug|release]

Build and install the APK directly via adb.
  debug   (default) Build debug APK and install
  release Build release APK and install
EOF
    exit 1
}

MODE="${1:-debug}"

case "$MODE" in
    debug|release) ;;
    *) usage ;;
esac

cd "$(dirname "$0")"

echo "==> Building $MODE APK..."
./gradlew "assemble$MODE" --daemon

APK_DIR="app/build/outputs/apk/$MODE"
APK=$(ls "$APK_DIR"/*.apk 2>/dev/null | head -1)

if [ -z "$APK" ]; then
    echo "ERROR: No APK found in $APK_DIR" >&2
    exit 1
fi

echo "==> APK: $APK"
echo "==> Installing via adb..."
adb install -r "$APK"

echo "==> Launching app..."
adb shell monkey -p com.klyx -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

echo "==> Done."
