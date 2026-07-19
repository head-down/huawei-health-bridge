#!/bin/bash
# Gradle wrapper — bypasses corrupted gradle-wrapper.jar
# Uses the Java home configured in local.properties, falling back to IntelliJ JBR.

# Override whatever JAVA_HOME the host shell has set
JAVA_HOME=""
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

# Accept an explicit JAVA_HOME override before looking for defaults
if [ -z "$GRADLE_JAVA_HOME" ]; then
    # Try IntelliJ JBR (the standard location on this machine)
    if [ -f "/d/software/IntelliJ IDEA 2025.3.4/jbr/bin/java.exe" ]; then
        JAVA_HOME="/d/software/IntelliJ IDEA 2025.3.4/jbr"
    fi
fi

if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: No JDK found. Set GRADLE_JAVA_HOME env var." >&2
    exit 1
fi

# Find Gradle distribution in wrapper cache
GRADLE_DIR=$(find "$GRADLE_USER_HOME/wrapper/dists/gradle-8.7-bin" -maxdepth 2 -name "gradle-8.7" -type d 2>/dev/null | head -1)

if [ -z "$GRADLE_DIR" ]; then
    echo "ERROR: Could not find cached Gradle 8.7 distribution." >&2
    exit 1
fi

export JAVA_HOME
exec "$JAVA_HOME/bin/java" \
    -classpath "$GRADLE_DIR/lib/gradle-launcher-8.7.jar" \
    org.gradle.launcher.GradleMain "$@"
