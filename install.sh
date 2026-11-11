#!/usr/bin/env bash
# Runs the Thaumaturge restore with the bundled runtime if present, then the first Java 17+ found:
# JAVA_HOME, PATH, then launcher-bundled runtimes.
# All arguments go to the installer; see ./install.sh --help.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
jar="$here/evo2-thaumaturge-restore.jar"
[ -f "$jar" ] || jar="$here/dist/evo2-thaumaturge-restore.jar"
if [ ! -f "$jar" ]; then
  echo "error: evo2-thaumaturge-restore.jar not found next to $0" >&2
  exit 1
fi

bundled="$here/runtime/bin/java"
if [ -x "$bundled" ]; then
  exec "$bundled" -jar "$jar" "$@"
fi

java_major() {
  "$1" -version 2>&1 | sed -n 's/.*version "\([0-9]*\)[."].*/\1/p' | head -n1
}

candidates=()
[ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME/bin/java")
command -v java >/dev/null 2>&1 && candidates+=("$(command -v java)")
shopt -s nullglob
for j in \
  "$HOME"/.local/share/PrismLauncher/java/*/bin/java \
  "$HOME"/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/*/bin/java \
  "$HOME"/.ftba/bin/runtime/*/bin/java \
  "$HOME"/.ftba/runtime/*/bin/java \
  "$HOME"/.minecraft/runtime/java-runtime-*/linux/java-runtime-*/bin/java \
  "$HOME"/.local/share/ModrinthApp/meta/java_versions/*/bin/java \
  /usr/lib/jvm/*/bin/java; do
  candidates+=("$j")
done
shopt -u nullglob

java=""
for j in "${candidates[@]}"; do
  [ -x "$j" ] || continue
  major="$(java_major "$j" || true)"
  if [ -n "$major" ] && [ "$major" -ge 17 ]; then
    java="$j"
    break
  fi
done

if [ -z "$java" ]; then
  echo "error: no Java 17 or newer found." >&2
  echo "Use the JRE 25 package, install Java 17+ (for example: sudo apt install openjdk-21-jre-headless)," >&2
  echo "or set JAVA_HOME." >&2
  exit 1
fi

exec "$java" -jar "$jar" "$@"
