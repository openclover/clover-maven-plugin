#!/bin/sh
find . -name target -type d -exec rm -fr {} \;
find . -name build.log -exec rm {} \;
find . -name output.log -exec rm {} \;
