#!/usr/bin/bash
#
# Deploy a release over SSH without downtime.
#
# The root directory should adhere to the following structure:
# $ROOT
# ├── current -> $ROOT/releases/RELEASE_TIMESTAMP_2
# └── releases
#     ├── RELEASE_TIMESTAMP_1
#     └── RELEASE_TIMESTAMP_2
#
# The latest release is always available as $ROOT/current.

set -euo pipefail

log_info() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')] [INFO] [cicd-utils/sshDeploy]: $*"
}

RELEASE=$(date +%s)
TARGET="${USER}@${TARGET_HOST}"

log_info "Preparing to release ${RELEASE}."
ssh -i "${SSH_KEY_FILE}" -oStrictHostKeyChecking=no "${TARGET}" "mkdir -p '${ROOT}/releases'"

log_info "Uploading release ${RELEASE}. This may take a while."
scp -i "${SSH_KEY_FILE}" -oStrictHostKeyChecking=no -qr "${SOURCE_DIRECTORY}" "${TARGET}:${ROOT}/releases/${RELEASE}"

# Atomically link $ROOT/current to the uploaded release on the target server.
# See https://temochka.com/blog/posts/2017/02/17/atomic-symlinks.html.
log_info "Promoting release ${RELEASE}."
ssh -i "${SSH_KEY_FILE}" -oStrictHostKeyChecking=no "${TARGET}" "
  ln -s '${ROOT}/releases/${RELEASE}' '${ROOT}/releases/current'
  mv -Tf '${ROOT}/releases/current' '${ROOT}/current'
"

log_info "Deleting old releases."
ssh -i "${SSH_KEY_FILE}" -oStrictHostKeyChecking=no "${TARGET}" "cd '${ROOT}/releases' && ls | sort -r | tail -n +6 | xargs -d '\n' -r rm -rf --"

log_info "Successfully deployed release ${RELEASE}."
