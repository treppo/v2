#!/usr/bin/env bash

set -eux

docker build --tag application --rm .
