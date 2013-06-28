#!/bin/sh

echo Checking Dependencies ------------------------------------------------

if [ ! -d ./lib ]; then
    mkdir -p ./lib;
fi;

if [ ! -f ./lib/avro-1.7.4.jar ]; then
  curl http://mirror.metrocast.net/apache/avro/avro-1.7.4/java/avro-1.7.4.jar -o ./lib/avro-1.7.4.jar
fi

if [ ! -f ./lib/jackson-core-asl-1.9.12.jar ]; then
  curl http://repo1.maven.org/maven2/org/codehaus/jackson/jackson-core-asl/1.9.12/jackson-core-asl-1.9.12.jar -o ./lib/jackson-core-asl-1.9.12.jar
fi

if [ ! -f ./lib/jackson-mapper-asl-1.9.12.jar ]; then
  curl http://repo1.maven.org/maven2/org/codehaus/jackson/jackson-mapper-asl/1.9.12/jackson-mapper-asl-1.9.12.jar -o ./lib/jackson-mapper-asl-1.9.12.jar 
fi

if [ ! -f ./lib/MarkdownDoclet.jar ]; then
  curl https://code.google.com/p/markdown-doclet/downloads/detail?name=MarkdownDoclet-3.0.jar -o ./lib/MarkdownDoclet.jar
fi


##From: http://stackoverflow.com/questions/7334754/correct-way-to-check-java-version-from-bash-script
if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME     
    _java="$JAVA_HOME/bin/java"
else
    echo "no java"
fi
if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" < "1.7" ]]; then
        echo
        echo
        echo ERROR ---------------- ERROR --------------- ERROR ---------- ERROR
        echo Must build with at least java 1.7 since fork/join tools are used 
        echo
        echo
        exit 1
    fi
fi

ant $1 

