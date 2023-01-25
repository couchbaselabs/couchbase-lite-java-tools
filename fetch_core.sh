#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
COMMON_DIR="${SCRIPT_DIR}/../common"
TOOLS_DIR="${SCRIPT_DIR}/jenkins"

rm -rf "${COMMON_DIR}/lite-core"

"${TOOLS_DIR}/fetch_core.sh" -d -e EE -p android
"${TOOLS_DIR}/fetch_core.sh" -d -e EE 

