import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile


class JenkinsContainer(imageName: String) : GenericContainer<JenkinsContainer>(DockerImageName.parse(imageName)) {

    init {
        withExposedPorts(8080)
        withCopyFileToContainer(
            MountableFile
                .forClasspathResource("secrets/jenkins-admin-password.txt"),
            "/var/jenkins_home/secrets/initialAdminPassword"
        )
    }

    fun getJenkinsUrl(): String {
        return "http://${containerIpAddress}:${getMappedPort(8080)}"
    }
}