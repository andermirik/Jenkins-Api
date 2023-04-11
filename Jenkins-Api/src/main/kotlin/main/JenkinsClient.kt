package main

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.client.JenkinsHttpClient
import com.offbytwo.jenkins.model.Build
import com.offbytwo.jenkins.model.BuildWithDetails
import com.offbytwo.jenkins.model.JobWithDetails
import com.offbytwo.jenkins.model.View
import main.XMLUtils.Companion.addJobParameterInConfigXml
import main.XMLUtils.Companion.deleteJobParameterInConfigXml
import main.XMLUtils.Companion.generateViewConfigXml
import main.XMLUtils.Companion.parseJobParametersFromConfigXml
import main.XMLUtils.Companion.updateJobParameterInConfigXml
import main.data.JenkinsServerInfo
import main.data.Job
import main.data.Plugin
import org.apache.http.message.BasicNameValuePair
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

@Service
class JenkinsClient(
    private val jenkinsServer: JenkinsServer,
    private val jenkinsHttpClient: JenkinsHttpClient,
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,

    @Value("jenkins.uri")
    private val uri: String
) {

    /**
     * 1.1 Получение информации о сервере Jenkins.
     *
     * @return Информация о сервере Jenkins в виде объекта JenkinsServerInfo.
     * @throws IOException в случае ошибки сети или аутентификации.
     */
    @Throws(IOException::class)
    fun getJenkinsServerInfo(): JenkinsServerInfo {
        val jsonResponse = jenkinsHttpClient.get("")
        return objectMapper.readValue(jsonResponse, JenkinsServerInfo::class.java)
    }

    /**
     * 1.2 Получение списка Job - getAllJobs
     * Возвращает список всех Job на сервере Jenkins.
     *
     * @return Список объектов Job, содержащих имя, URL, статус и класс каждой Job.
     */
    fun getAllJobs(): List<Job> {
        val jsonResponse = jenkinsHttpClient.get("/api/json?tree=jobs[name,url,color,_class]")
        val jenkinsServerInfo = objectMapper.readValue(jsonResponse, JenkinsServerInfo::class.java)
        return jenkinsServerInfo.jobs ?: emptyList()
    }

    /**
     * 1.3 Получение информации о Job.
     *
     * @param jobName Имя Job.
     * @return JobWithDetails объект с подробной информацией о задаче.
     * @throws IOException в случае ошибки при получении данных.
     */
    @Throws(IOException::class)
    fun getJobInfo(jobName: String): JobWithDetails {
        return jenkinsServer.getJob(jobName)
    }


    /**
     * 1.4 Запуск сборки Job.
     *
     * @param jobName Имя Job.
     * @throws IOException в случае ошибки при запуске сборки.
     */
    @Throws(IOException::class)
    fun buildJob(jobName: String) {
        jenkinsServer.getJob(jobName).build()
    }

    /**
     * Метод 1.5: Остановка сборки Job.
     *
     * @param jobName Имя Job.
     * @param buildNumber Номер сборки, которую нужно остановить.
     * @throws IOException в случае ошибки при остановке сборки.
     */
    @Throws(IOException::class)
    fun stopJobBuild(jobName: String, buildNumber: Int) {
        jenkinsServer.getJob(jobName).getBuildByNumber(buildNumber).Stop()
    }

    /**
     * 1.6 Получение списка сборок Job - getJobBuilds
     *
     * Получает список всех сборок задачи по имени задачи. Список включает информацию о номере сборки,
     * статусе, времени начала, идентификаторе, результате, продолжительности и оценочной продолжительности.
     *
     * @param jobName Имя задачи Jenkins.
     * @return Список сборок задачи.
     * @throws IOException Если возникает ошибка при получении данных.
     */
    @Throws(IOException::class)
    fun getJobBuilds(jobName: String): List<Build> {
        return jenkinsServer.getJob(jobName).builds
    }

    /**
     * 1.7 Получение информации о сборке Job - getBuildInfo
     *
     * Получает подробную информацию о сборке задачи по имени задачи и номеру сборки.
     *
     * @param jobName Имя задачи Jenkins.
     * @param buildNumber Номер сборки задачи.
     * @return Информация о сборке.
     * @throws IOException Если возникает ошибка при получении данных.
     */
    @Throws(IOException::class)
    fun getBuildInfo(jobName: String, buildNumber: Int): BuildWithDetails {
        return jenkinsServer.getJob(jobName).getBuildByNumber(buildNumber).details()
    }

    /**
     * 1.8 Получение консольного вывода сборки Job - getBuildConsoleOutput
     *
     * Получает консольный вывод для конкретной сборки задачи, используя имя задачи и номер сборки.
     *
     * @param jobName Имя задачи Jenkins.
     * @param buildNumber Номер сборки задачи.
     * @return Консольный вывод сборки.
     * @throws IOException Если возникает ошибка при получении данных.
     */
    @Throws(IOException::class)
    fun getBuildConsoleOutput(jobName: String, buildNumber: Int): String {
        return jenkinsServer.getJob(jobName).getBuildByNumber(buildNumber).details().consoleOutputText
    }

    /**
     * 1.9 Создание Job - createJob
     *
     * Создает новую задачу в Jenkins с заданным именем и конфигурацией.
     *
     * @param jobName Имя задачи Jenkins.
     * @param jobConfig XML-строка конфигурации задачи.
     * @throws IOException Если возникает ошибка при создании задачи.
     */
    @Throws(IOException::class)
    fun createJob(jobName: String, jobConfig: String) {
        jenkinsServer.createJob(jobName, jobConfig, true)
    }

    /**
     * 1.10 Копирование Job - copyJob
     *
     * Копирует существующую задачу в Jenkins с новым именем.
     *
     * @param sourceJobName Имя существующей задачи Jenkins.
     * @param newJobName Имя новой задачи Jenkins.
     * @throws IOException Если возникает ошибка при копировании задачи.
     */
    @Throws(IOException::class)
    fun copyJob(sourceJobName: String, newJobName: String) {
        jenkinsHttpClient.post("/createItem?name=$newJobName&mode=copy&from=$sourceJobName")
    }

    // 1.11 Перемещение Job - moveJob !!!
    @Throws(IOException::class)
    fun moveJob(jobName: String, newFolder: String) {
        // 1. Получение конфигурации Job
        val jobConfig = jenkinsServer.getJobXml(jobName)

        // 2. Создание Job с тем же именем и конфигурацией в новой папке
        jenkinsServer.createJob("$newFolder/$jobName", jobConfig)

        // 3. Удаление исходного Job
        jenkinsServer.deleteJob(jobName)
    }


    /**
     * 1.12 Переименование Job - renameJob
     *
     * Переименовывает существующую задачу в Jenkins.
     *
     * @param oldJobName Имя существующей задачи Jenkins.
     * @param newJobName Новое имя для задачи Jenkins.
     * @throws IOException Если возникает ошибка при переименовании задачи.
     */
    fun renameJob(oldJobName: String, newJobName: String) {
        jenkinsServer.renameJob(oldJobName, newJobName)
    }

    // 1.13 Получение списка параметров Job
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun getJobParameters(jobName: String?): Map<String, String> {
        val jobConfig = jenkinsServer.getJobXml(jobName)
        return parseJobParametersFromConfigXml(jobConfig)
    }

    // 1.14 Получение значения параметра Job
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun getJobParameterValue(jobName: String?, parameterName: String?): String? {
        val parameters = getJobParameters(jobName)
        return parameters[parameterName]
    }

    // 1.15 Изменение значения параметра Job
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun setJobParameterValue(jobName: String?, parameterName: String, parameterValue: String) {
        val jobConfig = jenkinsServer.getJobXml(jobName)
        val updatedJobConfig: String = updateJobParameterInConfigXml(jobConfig, parameterName, parameterValue)
        jenkinsServer.updateJob(jobName, updatedJobConfig)
    }

    // 1.16. Удаление параметра Job
    @Throws(IOException::class)
    fun deleteParameter(jobName: String, parameterName: String) {
        val configXml = jenkinsServer.getJobXml(jobName)
        val updatedConfigXml: String = deleteJobParameterInConfigXml(configXml, parameterName)
        jenkinsServer.updateJob(jobName, updatedConfigXml)
    }

    // 1.17 Создание нового параметра Job
    @Throws(
        IOException::class,
        ParserConfigurationException::class,
        SAXException::class,
        TransformerException::class
    )
    fun createJobParameter(jobName: String, parameterName: String, defaultValue: String) {
        val configXml = jenkinsServer.getJobXml(jobName)
        val updatedConfigXml: String = addJobParameterInConfigXml(configXml, parameterName, defaultValue)
        jenkinsServer.updateJob(jobName, updatedConfigXml)
    }

    /**
     * 1.18 Получение списка View - getAllViews
     *
     * Возвращает список всех доступных представлений (View) на сервере Jenkins.
     *
     * @return Список объектов View, содержащих информацию о каждом доступном представлении.
     * @throws IOException Если возникает ошибка при получении списка представлений.
     */
    @Throws(IOException::class)
    fun getViewList(): List<View> {
        return jenkinsServer.views.values.toList()
    }

    /**
     * 1.19 Получение информации о View - getViewInfo
     *
     * Возвращает подробную информацию о выбранном представлении (View) на сервере Jenkins.
     *
     * @param viewName Имя представления, о котором требуется получить информацию.
     * @return Объект View с подробной информацией о представлении.
     * @throws IOException Если возникает ошибка при получении информации о представлении.
     */
    @Throws(IOException::class)
    fun getViewInfo(viewName: String): View {
        return jenkinsServer.getView(viewName)
    }

    /**
     * 1.20 Создание новой View - createView
     *
     * Создает новое представление (View) на сервере Jenkins с указанным именем и списком задач.
     *
     * @param viewName Имя нового представления.
     * @param viewType Тип нового представления.
     * @param jobNames Список имен задач, которые должны быть включены в представление.
     * @throws IOException Если возникает ошибка при создании представления.
     */
    @Throws(IOException::class)
    fun createView(viewName: String, viewType: String, jobNames: List<String>) {
        val viewConfigXml = generateViewConfigXml(viewName, viewType, jobNames)
        jenkinsServer.createView(viewName, viewConfigXml)
    }

    /**
     * 1.21 Редактирование View - updateView
     *
     * Редактирует существующее представление (View) на сервере Jenkins с указанным именем и новым списком задач.
     *
     * @param viewName Имя представления для редактирования.
     * @param newJobNames Новый список имен задач, которые должны быть включены в представление.
     * @throws IOException Если возникает ошибка при редактировании представления.
     */
    @Throws(IOException::class)
    fun updateView(viewName: String, viewXml: String, newJobNames: List<String>) {
        jenkinsServer.updateView(viewName, viewXml)
    }

    /**
     * 1.22 Удаление View - deleteView
     *
     * Удаляет существующее представление (View) на сервере Jenkins с указанным именем.
     *
     * @param viewName Имя представления для удаления.
     * @throws IOException Если возникает ошибка при удалении представления.
     */
    @Throws(IOException::class)
    fun deleteView(viewName: String?) {
        jenkinsHttpClient.post("/view/$viewName/doDelete")
    }


    // 1.23 Получение списка доступных плагинов
    @Throws(IOException::class)
    fun getAvailablePlugins(): List<Plugin> {
        val url = "/pluginManager/available/api/json"
        val jsonContent = jenkinsHttpClient.get(url)
        val jsonNode: JsonNode = objectMapper.readTree(jsonContent)
        return objectMapper.readValue(jsonNode.path("plugins").toString(), object : TypeReference<List<Plugin>>() {})
    }

    // 1.24 Получение списка установленных плагинов
    @Throws(IOException::class)
    fun getInstalledPlugins(): List<Plugin> {
        val url = "/pluginManager/api/json?depth=1"
        val jsonContent = jenkinsHttpClient.get(url)
        val jsonNode: JsonNode = objectMapper.readTree(jsonContent)
        return objectMapper.readValue(jsonNode.path("plugins").toString(), object : TypeReference<List<Plugin>>() {})
    }


    // 1.25 Загрузка и установка плагина из Update Center
    @Throws(IOException::class)
    fun installPlugin(pluginId: String, version: String) {
        val url = "/pluginManager/installNecessaryPlugins?plugin.$pluginId=$version"
        jenkinsHttpClient.post(url)
    }

    /**
     * 1.26 Получение информации о плагине - getPluginInfo
     *
     * Получает информацию о плагине, установленном на сервере Jenkins.
     *
     * @param pluginId Имя плагина для получения информации.
     * @return Объект PluginInfo с информацией о плагине.
     * @throws IOException Если возникает ошибка при получении информации о плагине.
     */
    @Throws(IOException::class)
    fun getPluginInfo(pluginId: String): Plugin {
        val url = "/pluginManager/plugin/$pluginId/api/json"
        val jsonContent: String = jenkinsHttpClient.get(url)
        return objectMapper.readValue(jsonContent, Plugin::class.java)
    }

    /**
     * 1.27 Включение плагина - enablePlugin
     *
     * Включает плагин на сервере Jenkins.
     *
     * @param pluginId Имя плагина для включения.
     * @throws IOException Если возникает ошибка при включении плагина.
     */
    @Throws(IOException::class)
    fun enablePlugin(pluginId: String) {
        val url = "/pluginManager/enable/$pluginId"
        jenkinsHttpClient.post(url)
    }

    /**
     * 1.28 Отключение плагина - disablePlugin
     *
     * Отключает плагин на сервере Jenkins.
     *
     * @param pluginId Имя плагина для отключения.
     * @throws IOException Если возникает ошибка при отключении плагина.
     */
    @Throws(IOException::class)
    fun disablePlugin(pluginId: String) {
        val url = "/pluginManager/disable/$pluginId"
        jenkinsHttpClient.post(url)
    }

    /**
     * 1.29 Получение списка групп - getAllGroups
     *
     * Возвращает список всех групп пользователей на сервере Jenkins.
     *
     * @return Список групп пользователей.
     * @throws IOException Если возникает ошибка при получении списка групп.
     */
    @Throws(IOException::class)
    fun getAllGroups(): List<Group> {
        val jsonResponse = jenkinsHttpClient.get("/securityRealm/groups/api/json")
        val groupList = objectMapper.readValue(jsonResponse, GroupList::class.java)
        return groupList.groups
    }

    data class GroupList(val groups: List<Group>)

    data class Group(
        val name: String,
        val description: String,
        val members: List<String>
    )

    /**
     * 1.30 Получение информации о группе - getGroupInfo
     *
     * Возвращает подробную информацию о группе пользователей на сервере Jenkins.
     *
     * @param groupName Имя группы, для которой требуется получить информацию.
     * @return Информация о группе пользователей.
     * @throws IOException Если возникает ошибка при получении информации о группе.
     */
    @Throws(IOException::class)
    fun getGroupInfo(groupName: String): Group {
        val jsonResponse = jenkinsHttpClient.get("/securityRealm/group/$groupName/api/json")
        return objectMapper.readValue(jsonResponse, Group::class.java)
    }

    /**
     * 1.31 Создание новой группы - createGroup
     *
     * Создает новую группу пользователей на сервере Jenkins.
     *
     * @param newGroupName Имя новой группы.
     * @throws IOException Если возникает ошибка при создании новой группы.
     */
    @Throws(IOException::class)
    fun createGroup(newGroupName: String) {
        jenkinsHttpClient.post("/securityRealm/group/createGroup?name=$newGroupName")
    }

    /**
     * 1.32 Удаление группы - deleteGroup
     *
     * Удаляет существующую группу пользователей на сервере Jenkins.
     *
     * @param groupName Имя удаляемой группы.
     * @throws IOException Если возникает ошибка при удалении группы.
     */
    @Throws(IOException::class)
    fun deleteGroup(groupName: String) {
        jenkinsHttpClient.post("/securityRealm/group/deleteGroup?name=$groupName")
    }

    /**
     * 1.33 Добавление пользователя в группу - addUserToGroup
     *
     * Добавляет пользователя в существующую группу на сервере Jenkins.
     *
     * @param userName Имя пользователя, которого нужно добавить в группу.
     * @param groupName Имя группы, в которую добавляется пользователь.
     * @throws IOException Если возникает ошибка при добавлении пользователя в группу.
     */
    @Throws(IOException::class)
    fun addUserToGroup(userName: String, groupName: String) {
        jenkinsHttpClient.post_form(
            "/securityRealm/user/$userName/addToGroup/$groupName",
            mapOf(
                "username" to userName,
                "groupname" to groupName
            ), false
        )
    }

    /**
     * 1.34 Удаление пользователя из группы - removeUserFromGroup
     *
     * Удаляет пользователя из существующей группы на сервере Jenkins.
     *
     * @param userName Имя пользователя, которого нужно удалить из группы.
     * @param groupName Имя группы, из которой удаляется пользователь.
     * @throws IOException Если возникает ошибка при удалении пользователя из группы.
     */
    @Throws(IOException::class)
    fun removeUserFromGroup(userName: String, groupName: String) {
        jenkinsHttpClient.post_form(
            "/securityRealm/user/$userName/removeFromGroup/$groupName",
            mapOf(
                "username" to userName,
                "groupname" to groupName
            ), false
        )
    }

    /**
     * 1.35 Получение списка разрешений группы - getGroupPermissions
     *
     * Возвращает список разрешений, присвоенных определенной группе на сервере Jenkins.
     *
     * @param groupName Имя группы, для которой требуется получить разрешения.
     * @return Список разрешений группы.
     * @throws IOException Если возникает ошибка при получении списка разрешений группы.
     */
    @Throws(IOException::class)
    fun getGroupPermissions(groupName: String): List<String> {
        val jsonResponse = jenkinsHttpClient.get("/securityRealm/group/$groupName/permissions/api/json")
        val permissionsJson = objectMapper.readTree(jsonResponse)["permissions"]

        return permissionsJson.map { it["permission"].asText() }
    }

    /**
     * 1.36 Добавление разрешения для группы - addGroupPermission
     *
     * Добавляет разрешение для определенной группы на сервере Jenkins.
     *
     * @param groupName Имя группы, для которой нужно добавить разрешение.
     * @param permissionId Идентификатор разрешения для добавления.
     * @param impliedBy Необязательный параметр, указывающий на то, что разрешение является подразумеваемым.
     * @throws IOException Если возникает ошибка при добавлении разрешения для группы.
     */
    @Throws(IOException::class)
    fun addGroupPermission(groupName: String, permissionId: String, impliedBy: String? = null) {
        val params = mutableListOf("permissionId=$permissionId")
        impliedBy?.let { params.add("impliedBy=$it") }
        val formData = params.joinToString("&")

        jenkinsHttpClient.post_text("/securityRealm/group/$groupName/permissions/add", formData, false)
    }

    /**
     * 1.37 Удаление разрешения для группы - removeGroupPermission
     *
     * Удаляет разрешение для определенной группы на сервере Jenkins.
     *
     * @param groupName Имя группы, у которой нужно удалить разрешение.
     * @param permissionId Идентификатор разрешения для удаления.
     * @param impliedBy Необязательный параметр, указывающий на то, что разрешение является подразумеваемым.
     * @throws IOException Если возникает ошибка при удалении разрешения для группы.
     */
    @Throws(IOException::class)
    fun removeGroupPermission(groupName: String, permissionId: String, impliedBy: String? = null) {
        val uriBuilder = UriComponentsBuilder
            .fromHttpUrl("${uri}/securityRealm/group/$groupName/permissions/remove")
            .queryParam("permissionId", permissionId)
        impliedBy?.let { uriBuilder.queryParam("impliedBy", it) }

        val uri = uriBuilder.build().toUri()

        val response: ResponseEntity<String> =
            restTemplate.exchange(uri, HttpMethod.POST, HttpEntity.EMPTY, String::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw IOException("Error occurred while removing group permission")
        }
    }

    /**
     * 1.38 Получение списка пользователей - getAllUsers
     *
     * Возвращает список всех пользователей, зарегистрированных на сервере Jenkins.
     *
     * @return Список пользователей.
     * @throws IOException Если возникает ошибка при получении списка пользователей.
     */
    @Throws(IOException::class)
    fun getAllUsers(): List<User> {
        val jsonResponse = jenkinsHttpClient.get("/asynchPeople/api/json?depth=1")
        val usersData = objectMapper.readValue(jsonResponse, UsersData::class.java)
        return usersData.users
    }

    data class UsersData(val users: List<User>)
    data class User(val id: String, val fullName: String, val description: String)

    /**
     * 1.39 Получение информации о пользователе - getUserInfo
     *
     * Возвращает информацию о пользователе с указанным идентификатором.
     *
     * @param userId Идентификатор пользователя.
     * @return Информация о пользователе.
     * @throws IOException Если возникает ошибка при получении информации о пользователе.
     */
    @Throws(IOException::class)
    fun getUserInfo(userId: String): UserInfo {
        val jsonResponse = jenkinsHttpClient.get("/user/$userId/api/json?depth=1")
        return objectMapper.readValue(jsonResponse, UserInfo::class.java)
    }

    data class UserInfo(
        val id: String,
        val fullName: String,
        val description: String,
        val emailAddress: String
    )

    /**
     * 1.40 Получение списка узлов (агентов) - getAllNodes
     *
     * Возвращает список всех узлов (агентов) в Jenkins.
     *
     * @return Список узлов (агентов).
     * @throws IOException Если возникает ошибка при получении списка узлов (агентов).
     */
    @Throws(IOException::class)
    fun getAllNodes(): List<NodeInfo> {
        val jsonResponse = jenkinsHttpClient.get("/computer/api/json?depth=1")
        val nodesWrapper = objectMapper.readValue(jsonResponse, NodesWrapper::class.java)
        return nodesWrapper.nodes
    }

    data class NodesWrapper(
        val nodes: List<NodeInfo>
    )

    data class NodeInfo(
        val displayName: String,
        val idle: Boolean,
        val offline: Boolean,
        val temporaryOfflineCause: String?,
        val numExecutors: Int,
        val executorLoad: Float
    )

    /**
     * 1.41 Получение информации об узле (агенте) - getNodeInfo
     *
     * Возвращает подробную информацию о конкретном узле (агенте) Jenkins.
     *
     * @param nodeName Имя узла (агента).
     * @return Информация об узле (агенте).
     * @throws IOException Если возникает ошибка при получении информации об узле (агенте).
     */
    @Throws(IOException::class)
    fun getNodeInfo(nodeName: String): NodeInfo {
        val jsonResponse = jenkinsHttpClient.get("/computer/${nodeName}/api/json?depth=1")
        return objectMapper.readValue(jsonResponse, NodeInfo::class.java)
    }

    /**
     * 1.42 Создание нового узла (агента) - createNode
     *
     * Создает новый узел (агент) Jenkins с заданными параметрами.
     *
     * @param nodeName Имя нового узла (агента).
     * @param nodeConfig XML-конфигурация узла (агента).
     * @throws IOException Если возникает ошибка при создании нового узла (агента).
     */
    @Throws(IOException::class)
    fun createNode(nodeName: String, nodeConfig: String) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_XML

        val requestEntity = HttpEntity(nodeConfig, headers)

        val jenkinsUri = "${uri}/computer/doCreateItem?name=$nodeName"
        restTemplate.exchange(jenkinsUri, HttpMethod.POST, requestEntity, String::class.java)
    }

    /**
     * 1.43 Удаление узла (агента) - deleteNode
     *
     * Удаляет существующий узел (агент) Jenkins.
     *
     * @param nodeName Имя узла (агента), который нужно удалить.
     * @throws IOException Если возникает ошибка при удалении узла (агента).
     */
    @Throws(IOException::class)
    fun deleteNode(nodeName: String) {
        val path = "/computer/$nodeName/doDelete"
        jenkinsHttpClient.post(path, true)
    }

    /**
     * 1.44 Включение узла (агента) - enableNode
     *
     * Включает существующий узел (агент) Jenkins.
     *
     * @param nodeName Имя узла (агента), который нужно включить.
     * @throws IOException Если возникает ошибка при включении узла (агента).
     */
    @Throws(IOException::class)
    fun enableNode(nodeName: String) {
        val path = "/computer/$nodeName/toggleOffline?offline=false"
        jenkinsHttpClient.post(path, true)
    }

    /**
     * 1.45 Выключение узла (агента) - disableNode
     *
     * Отключает существующий узел (агент) Jenkins.
     *
     * @param nodeName Имя узла (агента), который нужно отключить.
     * @throws IOException Если возникает ошибка при отключении узла (агента).
     */
    @Throws(IOException::class)
    fun disableNode(nodeName: String) {
        val path = "/computer/$nodeName/toggleOffline?offline=true"
        jenkinsHttpClient.post(path, true)
    }

    /**
     * 1.46 Загрузка и установка плагина по ссылке - installPluginByUrl
     *
     * Загружает и устанавливает плагин Jenkins по указанной ссылке.
     *
     * @param pluginUrl Ссылка на файл плагина.
     * @throws IOException Если возникает ошибка при загрузке или установке плагина.
     */
    @Throws(IOException::class)
    fun installPluginByUrl(pluginUrl: String) {
        val data = listOf(BasicNameValuePair("url", pluginUrl))
        val path = "/pluginManager/installNecessaryPlugins"
        jenkinsHttpClient.post_form_with_result(path, data, true)
    }

    /**
     * 1.47 Получение информации о текущем конфигурационном файле Jenkins - getCurrentConfigFile
     *
     * Возвращает текущий конфигурационный файл Jenkins в формате XML.
     *
     * @return Строка, содержащая конфигурационный файл Jenkins в формате XML.
     * @throws IOException Если возникает ошибка при получении конфигурационного файла.
     */
    @Throws(IOException::class)
    fun getCurrentConfigFile(): String {
        return jenkinsHttpClient.get("config.xml")
    }

    /**
     * 1.48 Обновление конфигурационного файла Jenkins - updateConfigFile
     *
     * Обновляет конфигурационный файл Jenkins с использованием предоставленного XML-документа.
     *
     * @param configXml Строка, содержащая новый конфигурационный XML-документ.
     * @throws IOException Если возникает ошибка при обновлении конфигурационного файла.
     */
    @Throws(IOException::class)
    fun updateConfigFile(configXml: String) {
        jenkinsHttpClient.post_xml("config.xml", configXml)
    }

    data class JenkinsStatistics(
        val busyExecutors: Int,
        val queueLength: Int,
        val totalExecutors: Int,
        val totalQueueLength: Int
    )

    /**
     * 1.49 Получение статистики о сервере Jenkins - getJenkinsStatistics
     *
     * Возвращает статистическую информацию о сервере Jenkins, такую как количество выполненных заданий,
     * успешных и неуспешных сборок, и общее количество узлов.
     *
     * @return Объект JenkinsStatistics, содержащий информацию о статистике сервера Jenkins.
     * @throws IOException Если возникает ошибка при получении статистики сервера Jenkins.
     */
    @Throws(IOException::class)
    fun getJenkinsStatistics(): JenkinsStatistics {
        val responseJson = jenkinsHttpClient.get("overallLoad/api/json")
        return objectMapper.readValue(responseJson, JenkinsStatistics::class.java)
    }

    /**
     * 1.50 Запуск перезапуска сервера Jenkins - restartJenkinsServer
     *
     * Перезапускает сервер Jenkins.
     *
     * @throws IOException Если возникает ошибка при перезапуске сервера Jenkins.
     */
    @Throws(IOException::class)
    fun restartJenkins() {
        jenkinsServer.restart(false)
    }
}


