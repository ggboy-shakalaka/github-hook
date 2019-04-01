#!/bin/bash

baseUrl=/media/app
gitUrl=https://github.com/app.git

rm -rf ${baseUrl}
git clone ${gitUrl} ${baseUrl}
