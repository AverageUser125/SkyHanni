#!/usr/bin/env bash
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PG="/opt/proguard/bin/proguard.sh"
JAVA_HOME="${JAVA_HOME:?JAVA_HOME is not set}"

IN="$ROOT/build/libs/SkyHanni-7.46.0-mc26.2.jar"
OUT="$ROOT/build/libs/SkyHanni-7.46.0-mc26.2-optimized.jar"
RULES="$ROOT/proguard-rules.pro"
LIST="$ROOT/libraries.txt"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"

declare -A SEEN
LIB=()

die() {
    echo "ERROR: $*" >&2
    exit 1
}

add() {
    local f="$1"

    [[ -f "$f" ]] || return 0

    f="$(realpath "$f")"

    [[ "$f" == "$(realpath "$IN")" ]] && return 0

    case "$f" in
        *-sources.jar|*-javadoc.jar|*-src.jar)
            return 0
            ;;
    esac

    [[ "${SEEN[$f]+yes}" ]] && return 0

    SEEN["$f"]=1
    LIB+=("-libraryjars" "$f")
    printf '  + %s\n' "$f"
}

find_one() {
    local pattern="$1"
    find "$CACHE" -type f -name "$pattern" \
        ! -name '*-sources.jar' \
        ! -name '*-javadoc.jar' \
        ! -name '*-src.jar' \
        2>/dev/null | sort -V | tail -n 1
}

[[ -x "$PG" ]] || die "ProGuard not found: $PG"
[[ -f "$IN" ]] || die "Input JAR not found: $IN"
[[ -f "$RULES" ]] || die "Rules not found: $RULES"
[[ -f "$LIST" ]] || die "libraries.txt not found: $LIST"
[[ -f "$JAVA_HOME/jmods/java.base.jmod" ]] || die "Invalid JAVA_HOME: $JAVA_HOME"

rm -f "$OUT"

echo "== JDK =="

for m in \
    java.base \
    java.desktop \
    java.naming \
    java.security.jgss \
    java.management \
    java.logging \
    java.net.http \
    java.sql \
    java.xml \
    jdk.crypto.ec \
    jdk.management \
    jdk.unsupported
do
    add "$JAVA_HOME/jmods/$m.jmod"
done

echo
echo "== libraries.txt =="

COUNT=0

while IFS= read -r f || [[ -n "$f" ]]; do
    f="${f%$'\r'}"

    # Remove whitespace around the path.
    f="${f#"${f%%[![:space:]]*}"}"
    f="${f%"${f##*[![:space:]]}"}"

    [[ -z "$f" ]] && continue
    [[ "$f" == \#* ]] && continue

    # Only absolute paths.
    [[ "$f" == /* ]] || {
        echo "  ! invalid path: $f"
        continue
    }

    [[ -f "$f" ]] || {
        echo "  ! missing: $f"
        continue
    }

    add "$f"
    COUNT=$((COUNT + 1))
done < "$LIST"

echo "  Read $COUNT entries."

echo
echo "== Required Gradle dependencies =="

# Kotlin stdlib
KOTLIN="$(find_one 'kotlin-stdlib-*.jar')"
[[ -n "$KOTLIN" ]] && add "$KOTLIN"

# Kotlin reflect
REFLECT="$(find_one 'kotlin-reflect-*.jar')"
[[ -n "$REFLECT" ]] && add "$REFLECT"

# Coroutines
COROUTINES="$(find_one 'kotlinx-coroutines-core-jvm-*.jar')"
[[ -z "$COROUTINES" ]] && COROUTINES="$(find_one 'kotlinx-coroutines-core-*.jar')"
[[ -n "$COROUTINES" ]] && add "$COROUTINES"

# JetBrains annotations
ANNOTATIONS="$(find_one 'annotations-*.jar')"
[[ -n "$ANNOTATIONS" ]] && add "$ANNOTATIONS"

# Hypixel Mod API
HYPIXEL="$CACHE/net.hypixel/mod-api/1.0.2/2ad8cc0e370fc00d27540617817204019f911708/mod-api-1.0.2.jar"
add "$HYPIXEL"

# Fabric jars produced by the build
for f in "$ROOT"/build/libs/fabric-api-*.jar \
         "$ROOT"/build/libs/fabric-language-kotlin-*.jar \
         "$ROOT"/build/libs/modmenu-*.jar \
         "$ROOT"/build/libs/ModMenu-*.jar
do
    [[ -f "$f" ]] && add "$f"
done

echo
echo "============================================================"
echo "Input:     $IN"
echo "Output:    $OUT"
echo "Libraries: $((${#LIB[@]} / 2))"
echo "============================================================"
echo

echo "== Running ProGuard =="

if ! "$PG" \
    "@$RULES" \
    -injars "$IN" \
    -outjars "$OUT" \
    "${LIB[@]}"
then
    echo
    echo "ERROR: ProGuard failed."
    exit 1
fi

[[ -f "$OUT" ]] || die "ProGuard completed but output was not created."

echo
echo "============================================================"
echo "SUCCESS"
echo "============================================================"

ls -lh "$IN" "$OUT"

echo
echo "SHA256:"
sha256sum "$OUT"
