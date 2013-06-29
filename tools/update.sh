#!/bin/bash

# check the environment
which adb &>/dev/null
[ $? -eq 0 ] || { echo "adb command not found."; exit 1; }

which android &>/dev/null
[ $? -eq 0 ] || { echo "android command not found."; exit 1; }

which ant &>/dev/null
[ $? -eq 0 ] || { echo "ant command not found."; exit 1; }


if [ $# -eq 1 ]; then
	BASEDIR=$(dirname $0)
	java -cp ${BASEDIR}/lib/anttasks.jar com.dianping.ant.UpdateProject ${BASEDIR} $*
else
	echo "update.sh useage:"
	echo "	update.sh <workspace or project>"
fi
