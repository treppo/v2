#!/usr/bin/env bash

echo $HEROKU_API_KEY | docker login --username _ --password-stdin registry.heroku.com
docker tag application registry.heroku.com/yorck-ratings/web
docker push registry.heroku.com/yorck-ratings/web
heroku container:release web --app yorck-ratings
