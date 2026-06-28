#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") [debug|release]

Build and (optionally) install the APK.
  debug   (default) Build debug APK
  release Build release APK
  -i      Install via adb after building (e.g. ./build.sh release -i)
  -h      Show this help
EOF
    exit 1
}

MODE="debug"
INSTALL=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        debug|release) MODE="$1" ;;
        -i|--install) INSTALL=true ;;
        -h|--help) usage ;;
        *) usage ;;
    esac
    shift
done

cd "$(dirname "$0")"

VERSION=$(grep "^project.version=" gradle.properties | cut -d= -f2 | tr -d ' ')

echo "==> Building $MODE APK (v$VERSION)..."
./gradlew "assemble$MODE" --daemon

APK_DIR="app/build/outputs/apk/$MODE"
OUTPUT_DIR="output"
mkdir -p "$OUTPUT_DIR"

echo "==> Collecting APKs..."
COUNT=0
for apk in "$APK_DIR"/*.apk; do
    [ -f "$apk" ] || continue
    BASENAME=$(basename "$apk")

    ABI_LABEL=""
    case "$BASENAME" in
        *-arm64-v8a-*)   ABI_LABEL="arm64-v8a" ;;
        *-x86_64-*)      ABI_LABEL="x86_64" ;;
        *-universal-*)   ABI_LABEL="universal" ;;
    esac

    if [ -n "$ABI_LABEL" ]; then
        OUTPUT_NAME="klyx-${VERSION}-${ABI_LABEL}.apk"
    else
        OUTPUT_NAME="klyx-${VERSION}.apk"
    fi

    cp "$apk" "$OUTPUT_DIR/$OUTPUT_NAME"
    echo "  -> $OUTPUT_NAME  ($(du -h "$apk" | cut -f1))"
    COUNT=$((COUNT + 1))
done

echo "$VERSION" > "$OUTPUT_DIR/.version"

if [ "$COUNT" -eq 0 ]; then
    echo "ERROR: No APK found in $APK_DIR" >&2
    exit 1
fi

if [ "$INSTALL" = true ]; then
    VERSION=$(cat "$OUTPUT_DIR/.version" 2>/dev/null || echo "$VERSION")
    UNIVERSAL="$OUTPUT_DIR/klyx-${VERSION}-universal.apk"
    if [ -f "$UNIVERSAL" ]; then
        echo "==> Installing universal APK via adb..."
        adb install -r "$UNIVERSAL"
    else
        echo "==> Installing APK via adb..."
        FIRST=$(ls "$OUTPUT_DIR"/*.apk 2>/dev/null | head -1)
        adb install -r "${FIRST:-}"
    fi

    echo "==> Launching app..."
    adb shell monkey -p com.klyx -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
fi

echo "==> Done. APKs in $OUTPUT_DIR/"
