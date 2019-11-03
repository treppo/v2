#!/bin/sh

set -eux

docker run --publish 8000:8000 registry.heroku.com/yorck-ratings/web
