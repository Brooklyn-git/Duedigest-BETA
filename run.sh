#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/desktopApp/build/libs/desktopApp.jar"
if [ ! -f "$JAR" ]; then
    echo "JAR not found at $JAR. Run: ./gradlew desktopApp:jar"
    exit 1
fi
exec java --enable-native-access=ALL-UNNAMED -jar "$JAR" "$@"
