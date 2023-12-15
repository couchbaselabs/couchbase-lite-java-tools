#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."

cd "${ROOT_DIR}"

for dev in `adb devices | tail +2 | cut -f1`; do
    echo "========= $dev"
    export ANDROID_SERIAL="$dev"

    for app in com.couchbase.lite.kotlin.test.test com.couchbase.lite.kotlin.test com.couchbase.lite.test com.couchbase.lite.android.mobiletest; do
        adb shell pm uninstall -k --user 0 $app
    done
    
    gw :ee:android-ktx:ee_android-ktx:devTest
    cp -a ee/android-ktx/lib/build/reports/androidTests/connected ~/Desktop/"test-report-${dev}"
done
