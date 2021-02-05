import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.junit.Test

class SshDeploySpec extends JenkinsPipelineSpecification {
  def sshDeploy = null

  void setup() {
    sshDeploy = loadPipelineScriptForTest('vars/sshDeploy.groovy')
  }

  @Test
  void '[sshDeploy] uses requested credentials'() {
    when:
    sshDeploy(
      credentialsId: '<CREDENTIALS>',
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>'
    )

    then:
    1 * getPipelineMock('withCredentials')(['<REQUESTED_CREDENTIALS>'], _ as Closure)
    1 * getPipelineMock('sshUserPrivateKey.call')(
      credentialsId: '<CREDENTIALS>',
      keyFileVariable: 'SSH_KEY_FILE',
      usernameVariable: 'SSH_USER'
    ) >> '<REQUESTED_CREDENTIALS>'
  }

  @Test
  void '[sshDeploy] uploads a release through scp'() {
    when:
    sshDeploy(
      credentialsId: _,
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~ /RELEASE=\$\(.*\)/
      it.script =~ /ssh -i "\$\{SSH_KEY_FILE\}" -oStrictHostKeyChecking=no "\$\{SSH_USER\}@<TARGET_HOST>" "mkdir -p '<TARGET_ROOT>\/releases'"/
      it.script =~ /scp -i "\$\{SSH_KEY_FILE\}" -oStrictHostKeyChecking=no -qr "<SOURCE_DIR>" "\$\{SSH_USER\}@<TARGET_HOST>:<TARGET_ROOT>\/releases\/\$\{RELEASE\}"/
    })
  }

  @Test
  void '[sshDeploy] atomically marks the latest release as current'() {
    when:
    sshDeploy(
      credentialsId: _,
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~ /RELEASE=\$\(.*\)/
      it.script =~ /(?s)ssh -i "\$\{SSH_KEY_FILE\}" -oStrictHostKeyChecking=no "\$\{SSH_USER\}@<TARGET_HOST>" ".*ln -s '<TARGET_ROOT>\/releases\/\$\{RELEASE\}' '<TARGET_ROOT>\/releases\/current'.*"/
      it.script =~ /(?s)ssh -i "\$\{SSH_KEY_FILE\}" -oStrictHostKeyChecking=no "\$\{SSH_USER\}@<TARGET_HOST>" ".*mv -Tf '<TARGET_ROOT>\/releases\/current' '<TARGET_ROOT>\/current'.*"/
    })
  }

  @Test
  void '[sshDeploy] deletes old releases'() {
    when:
    sshDeploy(
      credentialsId: _,
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~ /ssh -i "\$\{SSH_KEY_FILE\}" -oStrictHostKeyChecking=no "\$\{SSH_USER\}@<TARGET_HOST>" "cd '<TARGET_ROOT>\/releases' && ls | sort -r | tail -n +6 | xargs -d '\\n' -r rm -rf --"/
    })
  }
}
