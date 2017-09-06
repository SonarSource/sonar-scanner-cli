#!/bin/bash -v

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v34 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install                                                                                                                                    
}
configureTravis

export DEPLOY_PULL_REQUEST=true

regular_mvn_build_deploy_analyze -Pdist-linux,dist-windows,dist-macosx

