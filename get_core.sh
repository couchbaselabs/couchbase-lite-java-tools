#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TOOLS_DIR="${SCRIPT_DIR}/../common/tools"


function usage() {
    echo "Usage: $0 <nexus url>"
    exit 1
}

if [ "$#" -ne 1 ]; then usage; fi

NEXUS_URL="$1"
if [ -z "${NEXUS_URL}" ]; then usage; fi

"${TOOLS_DIR}/clean_litecore.sh"
"${TOOLS_DIR}/get_java_core.sh" -e EE -n "${NEXUS_URL}"
"${TOOLS_DIR}/get_android_core.sh" -e EE -n "${NEXUS_URL}"

