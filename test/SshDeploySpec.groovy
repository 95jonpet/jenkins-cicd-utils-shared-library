import java.io.File
import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.junit.Test

class SshDeploySpec extends JenkinsPipelineSpecification {
  def sshDeploy = null

  void setup() {
    sshDeploy = loadPipelineScriptForTest('vars/sshDeploy.groovy')
  }

  @Test
  void '[sshDeploy] executes ssh-deploy.sh'() {
    when:
    sshDeploy(sourceDirectory: _, targetDeployRoot: _, targetHost: _, user: _)

    then:
    1 * getPipelineMock('libraryResource')('se/peterjonsson/cicd-utils/ssh-deploy.sh') >> '<ssh-deploy.sh>'
    1 * getPipelineMock('sh')({ it.script == '<ssh-deploy.sh>' })
  }

  @Test
  void '[sshDeploy] uploads a release through scp'() {
    given:
    1 * getPipelineMock('libraryResource')('se/peterjonsson/cicd-utils/ssh-deploy.sh') >> {
      def classLoader = getClass().getClassLoader();
      def file = new File(classLoader.getResource('se/peterjonsson/cicd-utils/ssh-deploy.sh').getFile())
      return file.getText('UTF-8')
    }

    when:
    sshDeploy(
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>',
      user: '<USER>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~ /RELEASE=\$\(.*\)/
      it.script =~ /TARGET="<USER>@<TARGET_HOST>"/
      it.script =~ /ssh "\$\{TARGET\}" "mkdir -p '<TARGET_ROOT>\/releases'"/
      it.script =~ /scp -qr "<SOURCE_DIR>" "\$\{TARGET\}:<TARGET_ROOT>\/releases\/\$\{RELEASE\}"/
    })
  }

  @Test
  void '[sshDeploy] atomically marks the latest release as current'() {
    given:
    1 * getPipelineMock('libraryResource')('se/peterjonsson/cicd-utils/ssh-deploy.sh') >> {
      def classLoader = getClass().getClassLoader();
      def file = new File(classLoader.getResource('se/peterjonsson/cicd-utils/ssh-deploy.sh').getFile())
      return file.getText('UTF-8')
    }

    when:
    sshDeploy(
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>',
      user: '<USER>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~ /RELEASE=\$\(.*\)/
      it.script =~
        /(?s)ssh "\$\{TARGET\}" ".*ln -s '<TARGET_ROOT>\/releases\/\$\{RELEASE\}' '<TARGET_ROOT>\/releases\/current'.*"/
      it.script =~ /(?s)ssh "\$\{TARGET\}" ".*mv -Tf '<TARGET_ROOT>\/releases\/current' '<TARGET_ROOT>\/current'.*"/
    })
  }

  @Test
  void '[sshDeploy] deletes old releases'() {
    given:
    1 * getPipelineMock('libraryResource')('se/peterjonsson/cicd-utils/ssh-deploy.sh') >> {
      def classLoader = getClass().getClassLoader();
      def file = new File(classLoader.getResource('se/peterjonsson/cicd-utils/ssh-deploy.sh').getFile())
      return file.getText('UTF-8')
    }

    when:
    sshDeploy(
      sourceDirectory: '<SOURCE_DIR>',
      targetDeployRoot: '<TARGET_ROOT>',
      targetHost: '<TARGET_HOST>',
      user: '<USER>'
    )

    then:
    1 * getPipelineMock('sh')({
      it.script =~
        /ssh "\$\{TARGET\}" "cd '<TARGET_ROOT>\/releases' && ls | sort -r | tail -n +6 | xargs -d '\\n' -r rm -rf --"/
    })
  }
}
