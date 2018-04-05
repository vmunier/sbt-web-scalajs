#!/bin/sh

baseDir="$( cd "$( dirname "$0" )" && pwd )"
cd $baseDir

expectedNbScalaFiles=$1
archive="$baseDir/server/target/universal/server-0.1.0-SNAPSHOT.zip"
unzip -o $archive
nbScalaFiles=$(unzip -l "server-0.1.0-SNAPSHOT/lib/*server*.jar" | grep ".*\.scala$" | wc -l)

echo "Number of Scala files: "$((nbScalaFiles))" ($expectedNbScalaFiles expected)"
[ "$nbScalaFiles" -eq "$expectedNbScalaFiles" ]
