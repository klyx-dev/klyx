#!/usr/bin/env bash

set -euo pipefail

VERSION="${1:-}"

if [[ -z "$VERSION" ]]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 0.1.0-alpha.1"
    exit 1
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+-alpha\.[0-9]+$ ]]; then
    echo "Invalid alpha version: $VERSION"
    echo "Expected format: 0.1.0-alpha.1"
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "Error: Working directory is not clean."
    exit 1
fi

if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo "Error: Tag $VERSION already exists."
    exit 1
fi

echo "Creating alpha release: $VERSION"

git tag "$VERSION"

echo "Pushing tag to origin..."

if ! git push origin "$VERSION"; then
    echo "Failed to push tag. Removing local tag..."
    git tag -d "$VERSION"
    exit 1
fi

echo ""
echo "Successfully pushed alpha tag: $VERSION"
