#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../.."
LATESTBUILDS="http://latestbuilds.service.couchbase.com/builds/latestbuilds/couchbase-lite-vector-search"

function usage() {
    echo "Usage: $0 "'<artifacts path>'
    exit 1
}

fetch_platform()
{
    platform="$1"
    src="$2"
    dstDir="$3"
    dstFile="$4"

    mkdir tmp
    curl -s "${LIB_URL}-${platform}.zip" -o tmp/lib.zip
    pushd tmp > /dev/null
    unzip -qq lib.zip > /dev/null
    DST_DIR="${ARTIFACTS}/${dstDir}"
    mkdir -p "${DST_DIR}"
    mv ${src} "${DST_DIR}/${dstFile}"
    popd >/dev/null
    rm -rf tmp
}

BUILD=`cat "${ROOT_DIR}/ext_release.txt"`
VERSION="${BUILD%-*}"
BUILD_NUMBER="${BUILD#*-}"
LIB_URL="${LATESTBUILDS}/${VERSION}/${BUILD_NUMBER}/couchbase-lite-vector-search-${BUILD}"

ARTIFACTS="$1"
if [ -z "$ARTIFACTS" ]; then usage; fi


echo "======== FETCH Extension Libraries v${VERSION}-${BUILD_NUMBER}"

echo "===== Android"
fetch_platform 'android-arm64-v8a' 'lib/CouchbaseLiteVectorSearch.so' 'android/arm64/arm64-v8a'
fetch_platform 'android-x86_64' 'lib/CouchbaseLiteVectorSearch.so' 'android/x86_64/x86_64'

echo "===== OSX"
fetch_platform 'apple' 'CouchbaseLiteVectorSearch.xcframework/macos-arm64_x86_64/CouchbaseLiteVectorSearch.framework/Versions/A/CouchbaseLiteVectorSearch' 'macos/universal/lib' 'CouchbaseLiteVectorSearch.dylib'

echo "===== Linux"
fetch_platform 'linux-x86_64' 'lib/CouchbaseLiteVectorSearch.so' 'linux/x86_64/lib'

echo "===== Windows"
fetch_platform 'windows-x86_64' 'bin/CouchbaseLiteVectorSearch.dll' 'windows/x86_64/lib'
fetch_platform 'windows-arm64' 'bin/CouchbaseLiteVectorSearch.dll' 'windows/arm64/lib'

