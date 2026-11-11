#!/usr/bin/env bash
# Builds the release artifacts: the plain jar plus one package per platform with Temurin JRE 25.
# Requires a JDK 17+ (for build.sh), curl, tar, unzip and zip.
set -euo pipefail

cd "$(dirname "$0")"
root="$(pwd)"
version="${VERSION:-$(git tag --sort=-version:refname 2>/dev/null | head -n1)}"
version="${version:-dev}"
out="$root/dist/release"
cache="$root/dist/jre-cache"
stage="$root/build/package"

jre_release="25.0.5+8"
jre_base="https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.5%2B8"
platforms=(
  "linux-x64|OpenJDK25U-jre_x64_linux_hotspot_25.0.5_8.tar.gz|6fac98f0c8202efc3135a9e9e42aa61963ec42d1c5cbb40caf231e7b14a24ef6"
  "linux-aarch64|OpenJDK25U-jre_aarch64_linux_hotspot_25.0.5_8.tar.gz|963931723891302ebb2687178532c3d793d71e2577cd058061c7efb396140a86"
  "windows-x64|OpenJDK25U-jre_x64_windows_hotspot_25.0.5_8.zip|da78d54a2fcdbba1e92d5e3f07dd30236f96297891c4a21a781c3f38f5c0beae"
  "macos-x64|OpenJDK25U-jre_x64_mac_hotspot_25.0.5_8.tar.gz|806902c796c670969b7b27df73b8f0fe0924a61bd65083aa53064115e49cab2d"
  "macos-aarch64|OpenJDK25U-jre_aarch64_mac_hotspot_25.0.5_8.tar.gz|72ebd76b502c849e88dea39afae51baf5f82f2b5b1b6389c28ea707b20451bce"
)

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

verify_sha256() {
  local actual
  actual="$(sha256 "$1")"
  if [ "$actual" != "$2" ]; then
    echo "error: SHA-256 mismatch for $1" >&2
    echo "  expected $2" >&2
    echo "  got      $actual" >&2
    return 1
  fi
}

download() {
  local url="$1" dest="$2"
  if [ -f "$dest" ]; then
    return
  fi
  echo "Downloading $(basename "$dest")"
  curl -fL --retry 3 --connect-timeout 30 -o "$dest.part" "$url"
  mv "$dest.part" "$dest"
}

extract_jre() {
  local platform="$1" archive="$2"
  local dest="$cache/extract/$platform" roots root
  rm -rf "$dest"
  mkdir -p "$dest"
  case "$archive" in
    *.zip) unzip -q "$archive" -d "$dest" ;;
    *.tar.gz) tar -xzf "$archive" -C "$dest" ;;
    *) echo "error: unsupported JRE archive: $archive" >&2; return 1 ;;
  esac
  roots=("$dest"/*)
  if [ "${#roots[@]}" -ne 1 ]; then
    echo "error: unexpected layout in $archive" >&2
    return 1
  fi
  root="${roots[0]}"
  if [ -d "$root/Contents/Home" ]; then
    root="$root/Contents/Home"
  fi
  if [ ! -x "$root/bin/java" ] && [ ! -f "$root/bin/java.exe" ]; then
    echo "error: no java in $archive" >&2
    return 1
  fi
  printf '%s\n' "$root"
}

copy_common() {
  local dest="$1"
  cp "$root/dist/evo2-thaumaturge-restore.jar" "$dest/"
  cp "$root/install.sh" "$root/install.ps1" "$root/LICENSE" "$root/LICENSE-ASM.txt" "$root/README.md" "$dest/"
  tr -d '\r' <"$root/install.cmd" | awk '{ printf "%s\r\n", $0 }' >"$dest/install.cmd"
}

rm -rf "$out"
mkdir -p "$out" "$cache"
./build.sh
mkdir -p "$stage"

copy_common "$out"

for record in "${platforms[@]}"; do
  IFS='|' read -r platform archive checksum <<<"$record"
  if [ -n "${PLATFORMS_ONLY:-}" ]; then
    case " $PLATFORMS_ONLY " in
      *" $platform "*) ;;
      *) continue ;;
    esac
  fi

  file="$cache/$archive"
  if [ -f "$file" ] && ! verify_sha256 "$file" "$checksum"; then
    rm -f "$file"
  fi
  download "$jre_base/$archive" "$file"
  verify_sha256 "$file" "$checksum"

  jre="$(extract_jre "$platform" "$file")"
  pkg="$stage/$platform"
  rm -rf "$pkg"
  mkdir -p "$pkg"
  copy_common "$pkg"
  cp -a "$jre" "$pkg/runtime"
  cat >"$pkg/THIRD_PARTY_NOTICES.txt" <<EOF
This package includes Eclipse Temurin JRE $jre_release, distributed under the
GNU General Public License version 2 with the Classpath Exception.
The full notices are in runtime/legal/.

Temurin: https://adoptium.net/
EOF

  case "$archive" in
    *.zip) (cd "$stage" && zip -qr "$out/evo2-thaumaturge-restore-$version-jre25-$platform.zip" "$platform") ;;
    *) (cd "$stage" && tar -czf "$out/evo2-thaumaturge-restore-$version-jre25-$platform.tar.gz" "$platform") ;;
  esac
done

(
  cd "$out"
  rm -f SHA256SUMS
  for file in *; do
    [ -f "$file" ] || continue
    [ "$file" = SHA256SUMS ] && continue
    printf '%s  %s\n' "$(sha256 "$file")" "$file"
  done >SHA256SUMS
)

echo "release artifacts: $out"
ls -lh "$out"
