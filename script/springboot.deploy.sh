#!/bin/bash

projectName=app
jarUrl=target
gitUrl=https://github.com/app.git
baseUrl=/media/${projectName}

rm -rf ${baseUrl}/${projectName}.jar ${baseUrl}/source

git clone $gitUrl ${baseUrl}/source
cd ${baseUrl}/source
mvn clean package
mv ${baseUrl}/source/${jarUrl}/${projectName}.jar ${baseUrl}/

pid=`ps -ef|grep ${projectName}.jar|grep -v grep|awk '{print $2}'`;
if [ "$pid" = "" ]; then
    echo "Nothing to kill";
else
    echo "kill $pid";
    kill -9 $pid;
fi

nohup java -jar ${baseUrl}/${projectName}.jar >> ${baseUrl}/start.log 2>&1 &
