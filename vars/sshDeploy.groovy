import static java.util.Objects.requireNonNull

/**
 * Deploy a directory over SSH while keeping multiple revisions.
 */
void call(Map config = [:]) {
  String sourceDirectory = requireNonNull(config.sourceDirectory, 'sourceDirectory must not be null')
  String targetDeployRoot = requireNonNull(config.targetDeployRoot, 'targetDeployRoot must not be null')
  String targetHost = requireNonNull(config.targetHost, 'targetHost must not be null')
  String user = requireNonNull(config.user, 'user must not be null')

  String script = libraryResource('se/peterjonsson/cicd-utils/ssh-deploy.sh')
    .replaceAll(/\$\{ROOT\}/, targetDeployRoot)
    .replaceAll(/\$\{SOURCE_DIRECTORY\}/, sourceDirectory)
    .replaceAll(/\$\{TARGET_HOST\}/, targetHost)
    .replaceAll(/\$\{USER\}/, user)

  sh(
    label: 'Deploy over SSH',
    script: script
  )
}
