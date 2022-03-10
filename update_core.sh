#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."

function usage() {
    echo "Usage: $0 <core commit> <ee commit>"
    exit 1
}

function fail() {
    echo "Aborting!!!"
    exit 1
}

if [ "$#" -ne 2 ]; then usage; fi

CE_COMMIT=$1
if [ -z "${CE_COMMIT}" ]; then usage; fi

EE_COMMIT=$2
if [ -z $EE_COMMIT ]; then usage; fi


# Make sure that Core and EE Core are in a reasonable state
pushd "${ROOT_DIR}" > /dev/null
cd core
if [[ $(git status -s --ignore-submodules=dirty) != '' ]]; then
    echo "Error: Core is dirty"
    fail
fi

cd ../couchbase-lite-core-EE
if [[ $(git status -s --ignore-submodules=dirty) != '' ]]; then
    echo "Error: EE Core is dirty"
    fail
fi
cd ..

# Verify that the artifacts exist
"${SCRIPT_DIR}/verify_core.sh" $CE_COMMIT $EE_COMMIT
if [[ $? -ne 0 ]]; then fail; fi

# Update Core
cd core
HEAD=`git rev-parse HEAD`
echo "=== Update Core: $HEAD -> $CE_COMMIT"
git checkout stable 2> /dev/null || git checkout -b stable
git reset --hard $HEAD
git remote prune origin
git fetch
git checkout working 2> /dev/null || git checkout -b working
git reset --hard $CE_COMMIT || fail
git submodule update --recursive

# Update EE Core
cd  ../couchbase-lite-core-EE
HEAD=`git rev-parse HEAD`
echo "=== Update EE Core: $HEAD -> $EE_COMMIT"
git checkout stable 2> /dev/null || git checkout -b stable
git reset --hard $HEAD
git remote prune origin
git fetch
git checkout working 2> /dev/null || git checkout -b working
git reset --hard "$EE_COMMIT" || fail
git submodule update --recursive

popd > /dev/null

echo "=== Fetch new core"
${SCRIPT_DIR}/fetch_core.sh

