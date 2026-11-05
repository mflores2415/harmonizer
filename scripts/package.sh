#!/usr/bin/env bash
# Builds a self-contained Harmonizer app (with its own Java runtime) for the current OS
# and zips it into target/dist. Used by the release workflow; also works locally.
#
#   scripts/package.sh [version]     e.g. scripts/package.sh 1.2.0
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${1:-1.0.0}"
# Public-domain demo songs shipped with the app.
DEMO_SONGS=(samples/ode-to-joy.mid samples/greensleeves.mid)

case "$(uname -s)" in
	MINGW* | MSYS* | CYGWIN*) OS=windows ;;
	Darwin) OS=macos ;;
	*) OS=linux ;;
esac

mvn -B -q -DskipTests package
cp target/harmonizer.jar target/app/
mkdir -p target/app/samples
cp "${DEMO_SONGS[@]}" target/app/samples/

rm -rf target/dist
jpackage \
	--type app-image \
	--name Harmonizer \
	--app-version "$VERSION" \
	--vendor "Harmonizer" \
	--description "Chord and note explorer for MIDI and karaoke files" \
	--input target/app \
	--main-jar harmonizer.jar \
	--main-class harmonizer.Harmonizer \
	--java-options "--enable-native-access=ALL-UNNAMED" \
	--dest target/dist

ARCHIVE="Harmonizer-$VERSION-$OS"
cd target/dist
case "$OS" in
	windows) jar --create --no-manifest --file "$ARCHIVE.zip" Harmonizer ;;
	macos) ditto -c -k --keepParent Harmonizer.app "$ARCHIVE.zip" ;;
	linux) tar -czf "$ARCHIVE.tar.gz" Harmonizer ;;
esac
echo "Built target/dist/$(ls "$ARCHIVE".zip "$ARCHIVE".tar.gz 2>/dev/null)"
