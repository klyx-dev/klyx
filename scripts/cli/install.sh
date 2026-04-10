#!/usr/bin/env bash

echo "Installing Klyx CLI..."

cat > $PREFIX/bin/klyx << 'EOF'
#!/usr/bin/env bash

TARGET="${1:-$PWD}"
TARGET=$(realpath "$TARGET")
ENCODED=$(printf '%s\n' "$TARGET" | sed 's/ /%20/g')

if [ -d "$TARGET" ]; then
    TYPE="project"
else
    TYPE="file"
fi

am start \
  -a android.intent.action.VIEW \
  -d "klyx://open?$TYPE=$ENCODED"
EOF

chmod +x $PREFIX/bin/klyx

echo "Done! You can now use: klyx ."
