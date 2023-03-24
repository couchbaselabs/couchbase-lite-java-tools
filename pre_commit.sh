#!/bin/sh
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null 2>&1 && pwd )"

pushd "$SCRIPT_DIR/.." > /dev/null 2>&1
ROOT=`pwd`

for mod in legal etc common ce "."; do
   cd "$ROOT/$mod"
   git remote prune origin
   git fetch
   echo ""
   echo ""
   echo "##########################"
   echo "###     $mod"
   echo "##########################"
   echo ""
   git status
   echo "================================================"
   echo ""
   git log -n 2
done

popd  > /dev/null 2>&1

