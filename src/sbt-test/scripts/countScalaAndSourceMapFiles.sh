#!/bin/sh

baseDir="$( cd "$( dirname "$0" )" && pwd )"
cd $baseDir

expectedNbScalaFiles=$1
expectedNbSourceMapFiles=$2

# Find archive in either sbt 1.x or sbt 2.x target layout
archive=$(find "$baseDir" -name "server-0.1.0-SNAPSHOT.zip" -type f 2>/dev/null | head -1)

if [ -z "$archive" ]; then
  echo "Could not find server-0.1.0-SNAPSHOT.zip"
  exit 1
fi

echo "Found archive: $archive"
unzip -o "$archive"
nbScalaFiles=$(unzip -l "server-0.1.0-SNAPSHOT/lib/*-assets.jar" | grep ".*\.scala$" | wc -l)
nbSourceMapFiles=$(unzip -l "server-0.1.0-SNAPSHOT/lib/*-assets.jar" | grep ".*\.map$" | wc -l)

echo "Number of Scala files: "$((nbScalaFiles))" ($expectedNbScalaFiles expected)"
echo "Number of Source Map files: "$((nbSourceMapFiles))" ($expectedNbSourceMapFiles expected)"
[ "$nbScalaFiles" -eq "$expectedNbScalaFiles" -a "$nbSourceMapFiles" -eq "$expectedNbSourceMapFiles" ]
