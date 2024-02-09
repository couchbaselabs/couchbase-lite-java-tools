#!/bin/sh
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

pushd "$SCRIPT_DIR/.." > /dev/null 2>&1
ROOT=`pwd`

for mod in ce common etc ''; do
    cd "${ROOT}/$mod"
    git clean -xdff -e 'local.*' -e '.idea' -e 'lite-core' -e 'ext-libs'
done

popd > /dev/null 2>&1

