#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TOOLS_DIR="${SCRIPT_DIR}/../common/tools"

"${TOOLS_DIR}/clean_litecore.sh"
"${TOOLS_DIR}/fetch_java_litecore.sh" -e EE
"${TOOLS_DIR}/fetch_android_litecore.sh" -e EE

