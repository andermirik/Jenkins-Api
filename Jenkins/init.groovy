import jenkins.model.*
import hudson.plugins.git.*
import hudson.model.*
import hudson.tasks.*
import hudson.util.Secret
import hudson.plugins.git.extensions.impl.*

def projectName = 'nir'
def project = Jenkins.instance.getItemByFullName(projectName)

if (project == null) {
    def repoUrl = 'https://git.ityce4ka.ru/andermirik/nir'
    def branch = 'master'
    def credentialsId = 'gitlab-credentials-id'

    project = new WorkflowJob(Jenkins.instance, projectName)
    Jenkins.instance.add(project, projectName)

    project.setDefinition(new CpsScmFlowDefinition(
            new GitSCM(
                    [
                            new UserRemoteConfig(repoUrl, null, credentialsId, null)
                    ],
                    [
                            new BranchSpec(branch)
                    ],
                    false,
                    [
                            new CloneOption(false, false, null, 1)
                    ],
                    null,
                    null,
                    null,
                    null,
                    [new CleanBeforeCheckout()]
            ),
            'Jenkinsfile'
    ))
    project.save()
}