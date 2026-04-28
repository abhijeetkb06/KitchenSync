#!/bin/bash
#
# CBLite Browser - Live document viewer for Couchbase Lite databases on Android emulators
#
# Auto-detects the app package and database name from the Android project
# this script lives in, then launches the cblite-browser viewer.
#
# Usage: ./launch_cblite_browser.sh
#
# Prerequisites:
#   - Android emulators running with the app installed
#   - cblite CLI installed (brew tap couchbase/tap && brew install cblite)
#   - Python 3 installed
#   - git installed (to auto-clone the cblite-browser repo if needed)
#

set -e

REPO_URL="https://github.com/abhijeetkb06/cblite-browser.git"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BROWSER_DIR="$SCRIPT_DIR/cblite-browser"

# --- Auto-detect app package from app/build.gradle ---
GRADLE_FILE="$SCRIPT_DIR/app/build.gradle"
if [ -f "$GRADLE_FILE" ]; then
    APP_PACKAGE=$(grep -m1 'applicationId' "$GRADLE_FILE" | sed 's/.*applicationId[[:space:]]*["'"'"']\([^"'"'"']*\)["'"'"'].*/\1/')
fi
if [ -z "$APP_PACKAGE" ]; then
    echo "ERROR: Could not detect applicationId from app/build.gradle"
    echo "       Make sure this script is in the root of an Android project."
    exit 1
fi

# --- Auto-detect database name from source code ---
DB_NAME=$(grep -r 'DATABASE_NAME\s*=' "$SCRIPT_DIR/app/src" 2>/dev/null \
    | grep -oE '"[^"]+"' | tr -d '"' | head -1)
if [ -z "$DB_NAME" ]; then
    # Fallback: use the last segment of the app package (e.g., com.kitchensync -> kitchensync)
    DB_NAME="${APP_PACKAGE##*.}"
fi

echo "Auto-detected from project:"
echo "  App package : $APP_PACKAGE"
echo "  DB name     : $DB_NAME"
echo ""

# --- Clone the cblite-browser repo if not already present ---
if [ ! -f "$BROWSER_DIR/server.py" ]; then
    echo "CBLite Browser not found locally. Cloning from GitHub..."
    git clone "$REPO_URL" "$BROWSER_DIR"
    echo ""
fi

# --- Launch ---
cd "$BROWSER_DIR"
exec bash launch.sh --app "$APP_PACKAGE" --dbname "$DB_NAME" --port 8091 --interval 3
