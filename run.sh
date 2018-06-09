#!/usr/bin/env bash

set -eux

docker run --rm --publish 8000:8000 --detach application
