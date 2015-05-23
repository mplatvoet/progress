#!/usr/bin/env bash
set -e

PROJECT_ROOT=`pwd`

DOCS_SOURCE="$PROJECT_ROOT/docs"

cd $DOCS_SOURCE
mkdocs build --clean

aws s3 sync $DOCS_SOURCE/site  s3://progress.komponents.nl --exclude ".DS_Store"

cd $PROJECT_ROOT