#!/bin/bash
#
# CBLite Browser - Live document viewer for Couchbase Lite databases on Android emulators
#
# Usage: ./launch_cblite_browser.sh
#
# Prerequisites:
#   - Android emulators running with the KitchenSync app installed
#   - cblite CLI installed (brew tap couchbase/tap && brew install cblite)
#   - Python 3 installed
#   - git installed (to auto-clone the cblite-browser repo if needed)
#
# This script:
#   1. Clones/locates the cblite-browser tool (https://github.com/abhijeetkb06/cblite-browser)
#   2. Launches it configured for KitchenSync (com.kitchensync, kitchensync.cblite2)
#   3. Opens the browser at http://localhost:8091
#

set -e

REPO_URL="https://github.com/abhijeetkb06/cblite-browser.git"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BROWSER_DIR="$SCRIPT_DIR/cblite-browser"

# Clone the cblite-browser repo if not already present
if [ ! -f "$BROWSER_DIR/server.py" ]; then
    echo "CBLite Browser not found locally. Cloning from GitHub..."
    git clone "$REPO_URL" "$BROWSER_DIR"
    echo ""
fi

# Delegate to the cblite-browser launch script with KitchenSync defaults
cd "$BROWSER_DIR"
exec bash launch.sh --app com.kitchensync --dbname kitchensync --port 8091 --interval 3
