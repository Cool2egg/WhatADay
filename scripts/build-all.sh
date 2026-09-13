#!/usr/bin/env bash
#
# 一键构建：前端 → 后端 → 单个可运行的 jar。
#
# 演示环境用这种方式：React 构建产物复制到后端的 static 目录，
# 由后端在 8080 端口统一提供页面与接口，不需要再单独跑前端服务。
#
# 用法：  bash scripts/build-all.sh
# 产物：  backend/target/whataday-<version>.jar
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> 1/3 构建前端"
(cd frontend && npm install --no-audit --no-fund && npm run build)

echo "==> 2/3 把前端产物复制到后端 static 目录"
STATIC_DIR="backend/src/main/resources/static"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r frontend/dist/. "$STATIC_DIR/"

echo "==> 3/3 打包后端"
(cd backend && mvn -B clean package -DskipTests)

echo ""
echo "构建完成，产物："
ls -1 "$ROOT"/backend/target/*.jar
echo ""
echo "运行： java -jar backend/target/whataday-*.jar"
echo "然后访问 http://127.0.0.1:8080"
