#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../.."
CORE_DIR="${ROOT_DIR}/common/lite-core"
DEBUG_OPT=""

function usage() {
   echo "usage: $0 -e EE|CE [-p <platform>] [-d] [-o <dir>]"
   echo "  -p|--platform     Core platform: android, windows, macos or linux. Default inferred from current OS"
   echo "  -e|--edition      LiteCore edition: CE or EE."
   echo "  -d|--debug        Fetch a debug version"
   echo "  -o|--output       Download target directory. Default is <root>/common/lite-core"
   echo
   exit 1
}

shopt -s nocasematch
while [[ $# -gt 0 ]]; do
   key="$1"
   case $key in
      -e|--edition)
         case "$2" in
            EE)
               EDITION=Enterprise
               EE_OPT='-EE'
               ;;
            CE)
               EDITION=Community
               EE_OPT=''
               ;;
            *)
               usage
               ;;
         esac
         shift
         shift
         ;;
      -p|--platform)
         PLATFORM="$2"
         shift
         shift
         ;;
      -d|--debug)
         DEBUG_OPT="-d"
         shift
         ;;
      -o|--output)
         CORE_DIR="$2"
         shift
         shift
         ;;
      *)
         echo >&2 "Unrecognized option $key, aborting..."
         usage
         ;;
   esac
done

if [ -z "${EDITION}" ]; then echo >&2 "Must specify an edition"; usage; fi

if [ -z "${PLATFORM}" ]; then PLATFORM=$OSTYPE; fi
case "${PLATFORM}" in
   android*)
      PLATFORM='android'
      ;;
   darwin*|mac*)
      PLATFORM='macos'
      ;;
   win*)
      PLATFORM=windows-win64
      ;;
   linux*)
      PLATFORM=linux
      ;;
   *)
      echo "Unsupported platform: ${PLATFORM}"
      usage
      ;;
esac

CORE_VERSION=`cat "${ROOT_DIR}/core_version.txt"`
if [ -z "${CORE_VERSION}" ]; then echo >&2 "Cannot get core version"; exit 1; fi
CORE_VERSION="${CORE_VERSION}${EE_OPT}"

rm -rf ./.penv
python3 -m venv ./.penv
. ./.penv/bin/activate

pip -q install GitPython

echo "=== Fetching artifacts for $( [ ! -z "${DEBUG_OPT}" ] && echo "Debug" )Core-${CORE_VERSION} ${EDITION} Edition on ${PLATFORM}"
mkdir -p "${CORE_DIR}"
python "${ROOT_DIR}/core_tools/fetch_litecore_version.py" $DEBUG_OPT -x "${ROOT_DIR}/etc/core" -b "${CORE_VERSION}" -v $PLATFORM -o "${CORE_DIR}"

deactivate
rm -rf ./.penv

# Ugly little kludge to get the Windows DLL to the right place
if [ $PLATFORM == "windows-win64" ]; then
   cp "${CORE_DIR}/windows/x86_64/bin"/*.dll "${CORE_DIR}/windows/x86_64/lib"
fi

