#!/usr/bin/env bash
# Builds dist/evo2-thaumaturge-restore.jar with ASM 9.9.1 bundled. Needs a JDK 17+ and curl.
set -euo pipefail

cd "$(dirname "$0")"
ASM_VERSION=9.9.1
ASM_SHA1=2ceea6ab43bcae1979b2a6d85fc0ca429877e5ab
ASM_JAR="lib/asm-$ASM_VERSION.jar"

mkdir -p lib
if [ ! -f "$ASM_JAR" ]; then
  curl -fsSL -o "$ASM_JAR.tmp" "https://repo1.maven.org/maven2/org/ow2/asm/asm/$ASM_VERSION/asm-$ASM_VERSION.jar"
  mv "$ASM_JAR.tmp" "$ASM_JAR"
fi
echo "$ASM_SHA1  $ASM_JAR" | sha1sum -c --quiet

rm -rf build
mkdir -p build/classes dist
find src -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  -cp "$ASM_JAR" -d build/classes
(cd build/classes && unzip -q -o "../../$ASM_JAR" 'org/*')
cp LICENSE-ASM.txt build/classes/
printf 'Main-Class: evo2restore.Main\n' > build/manifest.txt
jar --create --file dist/evo2-thaumaturge-restore.jar --manifest build/manifest.txt \
  --date 2026-11-06T00:00:00Z -C build/classes .
echo "built dist/evo2-thaumaturge-restore.jar"
