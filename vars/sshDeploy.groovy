import static java.util.Objects.requireNonNull

/**
 * Deploy a directory over SSH while keeping multiple revisions.
 */
void call(Map config = [:]) {
  String credentialsId = requireNonNull(config.credentialsId, 'credentialsId must not be null')
  String sourceDirectory = requireNonNull(config.sourceDirectory, 'sourceDirectory must not be null')
  String targetDeployRoot = requireNonNull(config.targetDeployRoot, 'targetDeployRoot must not be null')
  String targetHost = requireNonNull(config.targetHost, 'targetHost must not be null')

  // Shared options for ssh and scp.
  String sshOptions = [
    '-oStrictHostKeyChecking=no'
  ].join(' ')

  withCredentials([
    sshUserPrivateKey(
      credentialsId: credentialsId,
      keyFileVariable: 'SSH_KEY_FILE',
      usernameVariable: 'SSH_USER'
    )
  ]) {
    String logInfoPrefix = '[\$(date +\'%Y-%m-%dT%H:%M:%S%z\')] [INFO] [cicd-utils/sshDeploy]'
    String target = "\${SSH_USER}@${targetHost}"

    sh(
      label: 'Deploy over SSH',
      script: """
        set -euo pipefail

        RELEASE=\$(date +%s)
        echo "$logInfoPrefix: Preparing to release \${RELEASE}."
        ssh -i "\${SSH_KEY_FILE}" $sshOptions "$target" "mkdir -p '$targetDeployRoot/releases'"

        echo "$logInfoPrefix: Uploading release \${RELEASE}. This may take a while."
        scp -i "\${SSH_KEY_FILE}" $sshOptions -qr "${sourceDirectory}" "$target:$targetDeployRoot/releases/\${RELEASE}"

        echo "$logInfoPrefix: Promoting release \${RELEASE}."
        ssh -i "\${SSH_KEY_FILE}" $sshOptions "$target" "
          ln -s '$targetDeployRoot/releases/\${RELEASE}' '$targetDeployRoot/releases/current'
          mv -Tf '$targetDeployRoot/releases/current' '$targetDeployRoot/current'
        "

        echo "$logInfoPrefix: Deleting old releases."
        ssh -i "\${SSH_KEY_FILE}" $sshOptions "$target" "
          cd '$targetDeployRoot/releases' && ls | sort -r | tail -n +6 | xargs -d '\n' -r rm -rf --
        "

        echo "$logInfoPrefix: Successfully deployed release \${RELEASE}."
      """
    )
  }
}
