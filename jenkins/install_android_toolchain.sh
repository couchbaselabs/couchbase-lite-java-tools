#!/bin/bash
#
# Install the Android toolchain
# Don't execute this file, source it
# This script expects cbdep on the PATH and to inherit the variables:
#    BIN_DIR - the name of a directory into which it can install tools
#    ANDROID_HOME - the root of the android sdk.

# These versions must match the versions in lib/build.gradle
## Should update to at least:
# BUILD_TOOLS_VERSION='36.0.0'
# NDK_VERSION='28.0.13004108'
# CMAKE_VERSION='3.30.3
BUILD_TOOLS_VERSION='36.0.0'
NDK_VERSION='27.2.12479018'
NINJA_VERSION="1.10.2"
CMAKE_VERSION='3.30.3'

cbdep install -d "${BIN_DIR}" ninja ${NINJA_VERSION}
NINJA_DIR=`echo "${BIN_DIR}"/ninja-*`
PATH="${NINJA_DIR}/bin:${PATH}"

cbdep install -d "${BIN_DIR}" cmake ${CMAKE_VERSION}
CMAKE_DIR=`echo "${BIN_DIR}"/cmake-*`
PATH="${CMAKE_DIR}/bin:${PATH}"

# !!! Workaround for a dumb bug in the AGP
ln -s "${NINJA_DIR}/bin/ninja" "${CMAKE_DIR}/bin/ninja"

SDK_MGR="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --channel=1"
yes | ${SDK_MGR} --licenses > /dev/null 2>&1
${SDK_MGR} --install "build-tools;${BUILD_TOOLS_VERSION}"
${SDK_MGR} --install "ndk;${NDK_VERSION}"

