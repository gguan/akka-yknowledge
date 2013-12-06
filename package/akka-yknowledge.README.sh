#!/bin/sh

CURRENT=$(date -u '+%F')

if [ "x${BUILD_NUMBER}" = "x" ]; then
    echo "WARNING: BUILD_NUMBER env var not defined, normally Hudson sets this." >&2
    echo "Setting to 0. export BUILD_NUMBER=n and repeat the build to override." >&2
    BUILD_NUMBER=0
fi

cat <<EOF
Akka YKnowledge

Version 1.0.${BUILD_NUMBER} (${CURRENT})

Source code hosted on Yahoo github:

https://git.corp.yahoo.com/gguan/akka-importer
