#!/bin/bash
# Locally publishes every supported play version for every supported scala version

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

PLAY_VERSION=2.8 sbt +test +publishLocal
PLAY_VERSION=2.9 sbt +test +publishLocal
PLAY_VERSION=3.0 sbt +test +publishLocal
