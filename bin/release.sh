#!/bin/sh

set -eu

docker run --rm -e HEROKU_API_KEY="${HEROKU_API_KEY}" dickeyxxx/heroku-cli:v7.0.98 heroku container:release web --app yorck-ratings
