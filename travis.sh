#!/bin/bash -v

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/latest | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install                                                                                                                                    
}
configureTravis


regular_mvn_build_deploy_analyze

