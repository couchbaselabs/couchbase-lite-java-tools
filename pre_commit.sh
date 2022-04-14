#!/bin/sh
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null 2>&1 && pwd )"

pushd "$SCRIPT_DIR/.." > /dev/null 2>&1
for sm in legal etc common ce "."; do
   cd $sm
   git fetch
   echo ""
   echo "============== `pwd`"
   git status
   echo ""
   git log -n 2
   cd ..
done
popd  > /dev/null 2>&1

