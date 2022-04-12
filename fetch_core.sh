#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TOOLS_DIR="${SCRIPT_DIR}/jenkins"

"${TOOLS_DIR}/fetch_core.sh" -d -e EE -p android
"${TOOLS_DIR}/fetch_core.sh" -d -e EE 

