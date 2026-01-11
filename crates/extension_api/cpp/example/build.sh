#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Building Klyx Extension...${NC}"

echo -e "${YELLOW}Cleaning previous builds...${NC}"
rm -rf output/
mkdir -p output/

echo -e "${YELLOW}Compiling to WebAssembly...${NC}"
cd ../
/opt/wasi-sdk/bin/clang++ extension_api/extension.c extension_api/extension_component_type.o example/main.cpp -o example/main.wasm -mexec-model=reactor -fno-exceptions
cd - > /dev/null

WASM_FILE_NAME="main"
WASM_FILE="${WASM_FILE_NAME}.wasm"
if [ ! -f "$WASM_FILE" ]; then
    echo -e "${RED}Error: WASM file not found at $WASM_FILE${NC}"
    exit 1
fi

mkdir -p lib
cp "$WASM_FILE" "lib/"

TEMP_DIR="output/temp"
mkdir -p "$TEMP_DIR/src"

cp "$WASM_FILE" "$TEMP_DIR/src/"
cp "extension.toml" "$TEMP_DIR/"

EXTENSION_ID=$(grep '^id = ' extension.toml | cut -d '"' -f 2)
EXTENSION_VERSION=$(grep '^version = ' extension.toml | cut -d '"' -f 2)

ZIP_NAME="${EXTENSION_ID}-${EXTENSION_VERSION}.zip"
echo -e "${YELLOW}Creating package: output/$ZIP_NAME${NC}"

cd "$TEMP_DIR"
zip -r "../../output/${ZIP_NAME}" .
cd - > /dev/null

rm -rf "$TEMP_DIR"

echo -e "${GREEN}Build complete!${NC}"
echo -e "${GREEN}Package created: output/$ZIP_NAME${NC}"
echo -e "${GREEN}Contents:${NC}"
echo -e "  - extension.toml"
echo -e "  - src/${WASM_FILE_NAME}.wasm"
