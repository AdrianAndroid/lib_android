#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

SNAPSHOT=true impl/build.sh \
    createArchive \
    -Pandroidx.enableAffectedModuleDetection=false \
    "$@"

echo "Completing $0 at $(date)"
