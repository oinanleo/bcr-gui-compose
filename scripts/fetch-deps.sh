#!/usr/bin/env bash
# 下载构建需要的第三方二进制（不走 Maven 的那些）。
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p app/libs

SHERPA_VERSION="2.0.2"
AAR="sherpa-onnx-${SHERPA_VERSION}.aar"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/${AAR}"

if [ -f "app/libs/${AAR}" ]; then
  echo "app/libs/${AAR} 已存在，跳过"
else
  echo "下载 ${AAR} (~50 MB)…"
  curl -fL --retry 3 -o "app/libs/${AAR}" "$URL"
fi

ls -lh app/libs
echo "完成。现在可以 ./gradlew :app:assembleDebug"
