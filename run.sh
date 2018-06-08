#!/usr/bin/env bash

set -eux

docker run --detach --publish 8000:8000 application
