#!/usr/bin/env bash
set -e

PROJECT_ROOT=`pwd`

DOCS_SOURCE="$PROJECT_ROOT/docs"

BUILD_ROOT="$PROJECT_ROOT/build/gh-pages"
REPOSITORY_ROOT="$BUILD_ROOT/repository"

echo "Clearing build directory $BUILD_ROOT"
rm -rf $BUILD_ROOT

mkdir $BUILD_ROOT

cd $DOCS_SOURCE
mkdocs build --clean

aws s3 sync $DOCS_SOURCE/site  s3://progress.komponents.nl --exclude ".DS_Store"

cd $REPOSITORY_ROOT