#!/bin/sh

baseDir="$( cd "$( dirname "$0" )" && pwd )"
cd $baseDir

expectedNbScalaFiles=$1
expectedNbSourceMapFiles=$2
archive="$baseDir/server/target/universal/server-0.1.0-SNAPSHOT.zip"
unzip -o $archive
nbScalaFiles=$(unzip -l "server-0.1.0-SNAPSHOT/lib/*-assets.jar" | grep ".*\.scala$" | wc -l)
nbSourceMapFiles=$(unzip -l "server-0.1.0-SNAPSHOT/lib/*-assets.jar" | grep ".*\.map$" | wc -l)

echo "Number of Scala files: "$((nbScalaFiles))" ($expectedNbScalaFiles expected)"
echo "Number of Source Map files: "$((nbSourceMapFiles))" ($expectedNbSourceMapFiles expected)"
[ "$nbScalaFiles" -eq "$expectedNbScalaFiles" -a "$nbSourceMapFiles" -eq "$expectedNbSourceMapFiles" ]
