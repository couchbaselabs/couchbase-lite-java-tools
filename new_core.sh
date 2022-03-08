#!/bin/bash

NEXUS_URL="http://nexus.build.couchbase.com:8081/nexus/content/repositories/releases/com/couchbase/litecore"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."
TOOLS_DIR="${ROOT_DIR}/common/tools"
STATUS=0


function usage() {
    echo "Usage: $0 <core commit> <ee commit>"
    exit 1
}

test_core() {
   OS=$1
   EDITION=$2
   SHA=$3
   SUFFIX=$4
   echo -n "${OS} ${EDITION}(${SHA}): "
   curl -I -s -f -o /dev/null "${NEXUS_URL}/couchbase-litecore-${OS}/${SHA}/couchbase-litecore-${OS}-${SHA}.${SUFFIX}" -o "${OS}-${EDITION}.${SUFFIX}"
   if [ $? -eq 0 ]; then
      echo "Succeeded"
   else
      echo "Failed"
      STATUS=67
   fi
}

if [ "$#" -ne 2 ]; then usage; fi

CORE_ID="$1"
if [ -z "$CORE_ID" ]; then usage; fi

CORE_EE_ID="$2"
if [ -z "$CORE_EE_ID" ]; then usage; fi

CORE_EE_ID=`echo -n "${CORE_ID}${CORE_EE_ID}" | shasum -a 1`
CORE_EE_ID=${CORE_EE_ID:0:40}

rm -rf .core-tmp
mkdir .core-tmp
pushd .core-tmp > /dev/null

# Verify linux artifacts
test_core "linux" "CE" "${CORE_ID}" "tar.gz"
test_core "linux" "EE" "${CORE_EE_ID}" "tar.gz"

# Verify macos and windows artifacts
for OS in macosx windows-win64; do
   test_core "${OS}" "CE" "${CORE_ID}" "zip"
   test_core "${OS}" "EE" "${CORE_EE_ID}" "zip"
done

# Verify android artifacts
for ABI in armeabi-v7a arm64-v8a x86 x86_64; do
   test_core "android-${ABI}" "CE" "${CORE_ID}" "zip"
   test_core "android-${ABI}" "EE" "${CORE_EE_ID}" "zip"
done

popd > /dev/null
rm -rf .core-tmp

if [ "${STATUS}" -ne 0 ]; then exit $STATUS; fi

echo "CE: $CORE_ID" > "${ROOT_DIR}/core_version.txt"
echo "EE: $CORE_EE_ID" >> "${ROOT_DIR}/core_version.txt"

"${SCRIPT_DIR}/get_core.sh" "${NEXUS_URL}"

echo "==== Core updated!"
cat "${ROOT_DIR}/core_version.txt"

