#!/bin/bash

NEXUS_URL="http://nexus.build.couchbase.com:8081/nexus/content/repositories/releases/com/couchbase/litecore"
STATUS=0

function usage() {
    echo "Usage: $0 <core commit> <ee commit>"
    exit 1
}

test_core() {
   OS=$1
   EDITION=$2
   ID=$3
   SUFFIX=$4
   echo -n "${OS} ${EDITION}: "
   curl -I -s -f -o /dev/null "${NEXUS_URL}/couchbase-litecore-${OS}/${ID}/couchbase-litecore-${OS}-${ID}.${SUFFIX}" -o "${OS}-${EDITION}.${SUFFIX}"
   if [ $? -eq 0 ]; then
      echo "Succeeded"
   else
      echo "Failed"
      STATUS=67
   fi
}


if [ "$#" -ne 2 ]; then usage; fi

CE_COMMIT=$1
if [ -z "${CE_COMMIT}" ]; then usage; fi

EE_COMMIT=$2
if [ -z $EE_COMMIT ]; then usage; fi

# calculate the artifact IDs
CE_ID=$CE_COMMIT
EE_ID=`echo -n ="${CE_COMMIT}${EE_COMMIT}" | shasum -a 1`
EE_ID=${EE_ID:0:40}

rm -rf .core-tmp
mkdir .core-tmp
pushd .core-tmp > /dev/null

test_core "linux" "CE" "${CE_ID}" "tar.gz"
test_core "linux" "EE" "${EE_ID}" "tar.gz"

for OS in macosx windows-win64; do
   test_core "${OS}" "CE" "${CE_ID}" "zip"
   test_core "${OS}" "EE" "${EE_ID}" "zip"
done

for ABI in armeabi-v7a arm64-v8a x86 x86_64; do
   test_core "android-${ABI}" "CE" "${CE_ID}" "zip"
   test_core "android-${ABI}" "EE" "${EE_ID}" "zip"
done

popd > /dev/null
rm -rf .core-tmp

exit $STATUS

