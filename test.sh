#!/usr/bin/env bash
# Compiles the sources with the tests and runs every test. Needs a JDK 17+ and fetches ASM through build.sh.
set -euo pipefail

cd "$(dirname "$0")"
ASM_JAR=lib/asm-9.9.1.jar
[ -f "$ASM_JAR" ] || ./build.sh >/dev/null

rm -rf build/test-classes
mkdir -p build/test-classes
find src test -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  -cp "$ASM_JAR" -d build/test-classes
cp="build/test-classes:$ASM_JAR"
java -cp "$cp" Json5Test
java -cp "$cp" evo2restore.RestoreTest build/test-work
