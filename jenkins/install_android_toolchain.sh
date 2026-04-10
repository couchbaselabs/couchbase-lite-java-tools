#!/bin/bash
#
# Install the Android toolchain
# Don't execute this file, source it
# This script expects cbdep on the PATH and to inherit the variables:
#    BIN_DIR - the name of a directory into which it can install tools
#    ANDROID_HOME - the root of the android sdk.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSIONS_TOML="${SCRIPT_DIR}/../../ee/android/gradle/libs.versions.toml"
NINJA_VERSION="1.10.2"

if ! command -v yq >/dev/null 2>&1; then
	echo "Error: yq is required but was not found on PATH." >&2
	return 1 2>/dev/null || exit 1
fi

BUILD_TOOLS_VERSION=$(yq .versions.buildTools "${VERSIONS_TOML}")
NDK_VERSION=$(yq .versions.ndk "${VERSIONS_TOML}")
CMAKE_VERSION=$(yq .versions.cmake "${VERSIONS_TOML}")

cbdep install -d "${BIN_DIR}" ninja ${NINJA_VERSION}
NINJA_DIR=`echo "${BIN_DIR}"/ninja-*`
PATH="${NINJA_DIR}/bin:${PATH}"

cbdep install -d "${BIN_DIR}" cmake ${CMAKE_VERSION}
CMAKE_DIR=`echo "${BIN_DIR}"/cmake-*`
PATH="${CMAKE_DIR}/bin:${PATH}"

# !!! Workaround for a dumb bug in the AGP
ln -s "${NINJA_DIR}/bin/ninja" "${CMAKE_DIR}/bin/ninja"

SDK_MGR="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
yes | ${SDK_MGR} --licenses > /dev/null 2>&1
echo "===== Installing Android Build Tools ${BUILD_TOOLS_VERSION} and NDK ${NDK_VERSION} ====="
${SDK_MGR} --channel=1 --install "build-tools;${BUILD_TOOLS_VERSION}"
${SDK_MGR} --channel=1 --install "ndk;${NDK_VERSION}"

