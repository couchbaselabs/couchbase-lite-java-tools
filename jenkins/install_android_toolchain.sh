#!/bin/bash
#
# Install the Android toolchain
# Don't execute this file, source it
# This script expects cbdep on the PATH and to inherit the variables:
#    BIN_DIR - the name of a directory into which it can install tools
#    ANDROID_HOME - the root of the android sdk.

# These versions must match the versions in lib/build.gradle
BUILD_TOOLS_VERSION='34.0.0'
NDK_VERSION='25.1.8937393'
NINJA_VERSION="1.10.2"
CMAKE_VERSION='3.25.0'

# NDK r23b: matches the NDK that builds libLiteCore.so, needed to source the ASan runtime
# and libc++_shared.so for the android_asan lib module's instrumented tests.
NDK_VERSION_ASAN='23.1.7779620'

cbdep install -d "${BIN_DIR}" ninja ${NINJA_VERSION}
NINJA_DIR=`echo "${BIN_DIR}"/ninja-*`
PATH="${NINJA_DIR}/bin:${PATH}"

cbdep install -d "${BIN_DIR}" cmake ${CMAKE_VERSION}
CMAKE_DIR=`echo "${BIN_DIR}"/cmake-*`
PATH="${CMAKE_DIR}/bin:${PATH}"

# !!! Workaround for a dumb bug in the AGP
ln -s "${NINJA_DIR}/bin/ninja" "${CMAKE_DIR}/bin/ninja"

SDK_MGR="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
yes | ${SDK_MGR} --channel=1 --licenses > /dev/null 2>&1
${SDK_MGR} --channel=1 --install "build-tools;${BUILD_TOOLS_VERSION}"
${SDK_MGR} --channel=1 --install "ndk;${NDK_VERSION}"
${SDK_MGR} --channel=1 --install "ndk;${NDK_VERSION_ASAN}"

