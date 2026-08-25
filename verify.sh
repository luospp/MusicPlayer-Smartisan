#!/usr/bin/env bash
set -e

# MusicPlayer-Smartisan 全量验证：构建 + lint
# 用法：bash verify.sh（或 ./verify.sh）
# 要求 JDK 17（AGP 7.4.2 / Kotlin 2.0.21 需要；机器默认 JDK 19 会报错）

if [ -z "${JAVA_HOME:-}" ]; then
  export JAVA_HOME=/Users/luoshipeng/Library/Java/JavaVirtualMachines/corretto-17.0.11/Contents/Home
fi

bash gradlew :app:assembleDebug :app:lintDebug --console=plain
