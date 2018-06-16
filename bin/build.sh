#!/bin/sh

set -eu

docker build --tag registry.heroku.com/yorck-ratings/web --rm .
echo ${HEROKU_API_KEY} | docker login --username _ --password-stdin registry.heroku.com
docker push registry.heroku.com/yorck-ratings/web
