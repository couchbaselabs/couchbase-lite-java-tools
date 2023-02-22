#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."

function usage() {
    echo "Usage: $0 <build-id>"
    echo "       <build-id> Core build id, e.g., 3.2.0-119"
    exit 1
}


if [ "$#" -ne 1 ]; then usage; fi

CORE_BUILD=$1
if [ -z "${CORE_BUILD}" ]; then usage; fi

pushd "${ROOT_DIR}" > /dev/null

echo "=== Update Artifacts"
echo "${CORE_BUILD}" > core_version.txt

etc/fetch_core.sh

popd > /dev/null

