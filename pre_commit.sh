#!/bin/sh
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null 2>&1 && pwd )"

pushd "$SCRIPT_DIR/.." > /dev/null 2>&1
ROOT=`pwd`

for mod in etc common ce "."; do
   cd "$ROOT/$mod"
   echo ""
   echo "##########################"
   echo "###     $mod"
   echo "##########################"
   echo ""
   git remote prune origin
   git fetch -p -P
   echo ""
   git status
   echo "================================================"
   echo ""
   git log -n 2
done

popd  > /dev/null 2>&1

