#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."

pushd "${ROOT_DIR}" > /dev/null

# Make sure that Core and EE Core are in a reasonable state
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

# Update Core
cd core
git fetch
HEAD=`git rev-parse HEAD`
CE_COMMIT=`git rev-parse origin/staging/master`
echo "=== Update Core: $HEAD -> $CE_COMMIT"
git checkout stable 2> /dev/null || git checkout -b stable
git reset --hard $HEAD
git checkout working 2> /dev/null || git checkout -b working
git reset --hard $CE_COMMIT || fail
git submodule update --recursive
git remote prune origin

# Update EE Core
cd  ../couchbase-lite-core-EE
git fetch
HEAD=`git rev-parse HEAD`
EE_COMMIT=`git rev-parse origin/master`
echo "=== Update EE Core: $HEAD -> $EE_COMMIT"
git checkout stable 2> /dev/null || git checkout -b stable
git reset --hard $HEAD
git checkout working 2> /dev/null || git checkout -b working
git reset --hard "$EE_COMMIT" || fail
git submodule update --recursive
git remote prune origin

echo "=== Update Artifacts"
cd  ..
rm -rf ./.penv
python3 -m venv ./.penv
. ./.penv/bin/activate
pip -q install GitPython
"${ROOT_DIR}/common/tools/clean_litecore.sh"
python core/scripts/fetch_litecore_version.py -x "${ROOT_DIR}/etc" --ee -v macos android -r core -o "${ROOT_DIR}/common/lite-core"
deactivate
rm -rf ./.penv

# Temporary: we need this until the build scripts use the new artifact downloader
cd core
git checkout HEAD^2

popd > /dev/null

