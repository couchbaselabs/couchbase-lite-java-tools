#!/bin/bash
shopt -s extglob

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null 2>&1 && pwd )"

function usage() {
    echo "Usage: $0 <platform> <version> <suffix>"
    echo '       <platform> one of java, android'
    echo '       <version> the release version (x.y[...])'
    echo '       <suffix> release type: probably "beta" or "vf" or some such thing'
    exit 1
}

if [[ "$#" -gt 3 ]] || [[ "$#" -lt 2 ]]; then usage; fi
PLATFORM=$1
case $PLATFORM in
   java|android) ;;
   *) usage;;
esac

VERSION=$2
case $VERSION in
   +([0-9]).+([0-9])*) ;;
   *) usage;;
esac

SUFFIX=$3

TAG="${PLATFORM}/${VERSION}"

pushd "$SCRIPT_DIR/.." > /dev/null 2>&1
ROOT=`pwd`

for mod in etc common ce "."; do
    cd "$ROOT/$mod"
    git tag -a $TAG -m"${PLATFORM} version ${VERSION} ${SUFFIX}";
    git push origin $TAG
done

popd  > /dev/null 2>&1

