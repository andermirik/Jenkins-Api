import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.model.Job
import main.JenkinsRestApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI


@WebFluxTest(controllers = [JenkinsRestApi::class])
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JenkinsApiControllerTest(@Autowired private val webTestClient: WebTestClient) {

    private lateinit var jenkins: JenkinsContainer
    private lateinit var jenkinsServer: JenkinsServer

    @BeforeAll
    fun setup() {
        jenkins = JenkinsContainer("jenkins/jenkins:lts")
        jenkins.start()

        jenkinsServer = JenkinsServer(URI(jenkins.getJenkinsUrl()), "admin", "SUPERSECRET")
    }

    private fun getJenkinsUrl() = ""

    @AfterAll
    fun cleanup() {
        jenkins.stop()
    }

    private fun createJob(jobName: String): Job {
        val jobConfig = javaClass.getResource("/job-config.xml")?.readText()
        jenkinsServer.createJob(jobName, jobConfig, true)
        return jenkinsServer.getJob(jobName)
    }

    private fun createBuild(jobName: String, buildNumber: Int) {
        // Создайте сборку для задания в Jenkins с помощью JenkinsServer API
        val job = jenkinsServer.getJob(jobName)
        job.build()
    }

    private fun createTestView(viewName: String, viewType: String, jobNames: List<String>) {
        webTestClient.post()
            .uri("/api/jenkins/view/create?viewName=$viewName&viewType=$viewType")
            .bodyValue(jobNames)
            .exchange()
            .expectStatus().isOk
    }

    private fun updateTestView(viewName: String, viewXml: String, newJobNames: List<String>) {
        webTestClient.put()
            .uri("/api/jenkins/view/$viewName/update")
            .bodyValue(Pair(viewXml, newJobNames))
            .exchange()
            .expectStatus().isOk
    }

    private fun deleteTestView(viewName: String) {
        webTestClient.delete()
            .uri("/api/jenkins/view/$viewName/delete")
            .exchange()
            .expectStatus().isOk
    }

    private fun installTestPlugin(pluginId: String, version: String) {
        webTestClient.post()
            .uri("/api/jenkins/plugin/install?pluginId=$pluginId&version=$version")
            .exchange()
            .expectStatus().isOk
    }

    private fun createNode(nodeName: String) {
        val always = "$" + "Always"
        val nodeConfig = """
        <slave>
          <name>$nodeName</name>
          <description></description>
          <remoteFS>/tmp/$nodeName</remoteFS>
          <numExecutors>1</numExecutors>
          <mode>NORMAL</mode>
          <retentionStrategy class="hudson.slaves.RetentionStrategy$always"/>
          <launcher class="hudson.slaves.JNLPLauncher">
            <workDirSettings>
              <disabled>false</disabled>
              <internalDir>remoting</internalDir>
              <failIfWorkDirIsMissing>false</failIfWorkDirIsMissing>
            </workDirSettings>
          </launcher>
          <label></label>
          <nodeProperties/>
          <userId>null</userId>
        </slave>
    """.trimIndent()

        // Создание узла
        webTestClient.post()
            .uri("/api/jenkins/nodes?nodeName=$nodeName")
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(nodeConfig)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Create Node Test`() {
        val nodeName = "TestNode"
        val always = "$" + "Always"
        val nodeConfig = """
        <slave>
          <name>$nodeName</name>
          <description></description>
          <remoteFS>/tmp/$nodeName</remoteFS>
          <numExecutors>1</numExecutors>
          <mode>NORMAL</mode>
          <retentionStrategy class="hudson.slaves.RetentionStrategy$always"/>
          <launcher class="hudson.slaves.JNLPLauncher">
            <workDirSettings>
              <disabled>false</disabled>
              <internalDir>remoting</internalDir>
              <failIfWorkDirIsMissing>false</failIfWorkDirIsMissing>
            </workDirSettings>
          </launcher>
          <label></label>
          <nodeProperties/>
          <userId>null</userId>
        </slave>
    """.trimIndent()

        // Создание узла
        webTestClient.post()
            .uri("/api/jenkins/nodes?nodeName=$nodeName")
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(nodeConfig)
            .exchange()
            .expectStatus().isOk

        // Проверка наличия узла
        webTestClient.get()
            .uri("/api/jenkins/node/$nodeName/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.displayName").isEqualTo(nodeName)
    }

    @Test
    fun `Get Job Info Test`() {
        val jobName = "Job1"
        createJob(jobName)

        webTestClient.get()
            .uri("${getJenkinsUrl()}/api/jenkins/job/$jobName/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo(jobName)
            .jsonPath("$.url").isNotEmpty
            .jsonPath("$.builds").isNotEmpty
    }

    @Test
    fun `Get Build Info Test`() {
        val jobName = "Job1"
        createJob(jobName)
        val buildNumber = 1
        createBuild(jobName, buildNumber)

        webTestClient.get()
            .uri("${getJenkinsUrl()}/api/jenkins/job/$jobName/build/$buildNumber/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.number").isEqualTo(buildNumber)
            .jsonPath("$.url").isNotEmpty
            .jsonPath("$.result").isNotEmpty
    }

    @Test
    fun `Get Jenkins Server Info Test`() {
        webTestClient.get()
            .uri("${getJenkinsUrl()}/api/jenkins/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.serverUrl").isNotEmpty
            .jsonPath("$.version").isNotEmpty
            .jsonPath("$.mode").isNotEmpty
            .jsonPath("$.views").isNotEmpty
    }

    @Test
    fun `Enable Node Test`() {
        val nodeName = "TestNodeToEnable"

        // Создание узла
        createNode(nodeName)

        // Включение узла
        webTestClient.post()
            .uri("/api/jenkins/node/$nodeName/enable")
            .exchange()
            .expectStatus().isOk

        // Проверка, что узел включен
        webTestClient.get()
            .uri("/api/jenkins/node/$nodeName/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.offline").isEqualTo(false)
    }

    @Test
    fun `Disable Node Test`() {
        val nodeName = "TestNodeToDisable"

        // Создание узла
        createNode(nodeName)

        // Выключение узла
        webTestClient.post()
            .uri("/api/jenkins/node/$nodeName/disable")
            .exchange()
            .expectStatus().isOk

        // Проверка, что узел выключен
        webTestClient.get()
            .uri("/api/jenkins/node/$nodeName/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.offline").isEqualTo(true)
    }

    @Test
    fun `Install Plugin By Url Test`() {
        val pluginUrl = "https://example.com/plugin.hpi"

        // Установка плагина по URL
        webTestClient.post()
            .uri("/api/jenkins/plugin/install?pluginUrl=$pluginUrl")
            .exchange()
            .expectStatus().isOk

        // Здесь проверить установленный плагин может быть сложно из-за асинхронного процесса установки
    }

    @Test
    fun `Get Current Config File Test`() {
        webTestClient.get()
            .uri("/api/jenkins/config")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith { response ->
                assertThat(response.responseBody).isNotNull()
            }
    }

    @Test
    fun `Update Config File Test`() {
        val configXml = "<jenkins></jenkins>"

        webTestClient.put()
            .uri("/api/jenkins/config")
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(configXml)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Get Jenkins Statistics Test`() {
        webTestClient.get()
            .uri("/api/jenkins/statistics")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalJobs").isNotEmpty
            .jsonPath("$.successfulBuilds").isNotEmpty
            .jsonPath("$.failedBuilds").isNotEmpty
            .jsonPath("$.totalNodes").isNotEmpty
    }

    @Test
    fun `Restart Jenkins Test`() {
        webTestClient.post()
            .uri("/api/jenkins/restart")
            .exchange()
            .expectStatus().isOk

        // Тут проверить перезапуск сервера Jenkins может быть сложно, так как это может привести к прекращению работы теста
    }

    @Test
    fun `Create View Test`() {
        val viewName = "TestView"
        val viewType = "hudson.model.ListView"
        val jobNames = listOf("Job1", "Job2")

        createTestView(viewName, viewType, jobNames)

        webTestClient.get()
            .uri("/api/jenkins/view/$viewName")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo(viewName)
    }

    @Test
    fun `Install Plugin Test`() {
        val pluginId = "some-plugin"
        val version = "1.0.0"

        installTestPlugin(pluginId, version)

        webTestClient.get()
            .uri("/api/jenkins/plugin/$pluginId/info")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.shortName").isEqualTo(pluginId)
    }

    @Test
    fun `Update View Test`() {
        val viewName = "TestView"
        val viewType = "hudson.model.ListView"
        val jobNames = listOf("Job1", "Job2")

        // Создание тестового представления
        createTestView(viewName, viewType, jobNames)

        // Обновление тестового представления
        val updatedViewXml = "<hudson.model.ListView><name>$viewName</name></hudson.model.ListView>"
        val newJobNames = listOf("Job3", "Job4")
        updateTestView(viewName, updatedViewXml, newJobNames)

        // Проверка обновления представления
        webTestClient.get()
            .uri("/api/jenkins/view/$viewName")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo(viewName)
    }

    @Test
    fun `Delete View Test`() {
        val viewName = "TestView"
        val viewType = "hudson.model.ListView"
        val jobNames = listOf("Job1", "Job2")

        // Создание тестового представления
        createTestView(viewName, viewType, jobNames)

        // Удаление тестового представления
        deleteTestView(viewName)

        // Проверка удаления представления
        webTestClient.get()
            .uri("/api/jenkins/view/$viewName")
            .exchange()
            .expectStatus().isNotFound
    }

}