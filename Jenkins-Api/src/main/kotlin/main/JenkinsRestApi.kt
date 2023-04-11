package main

import com.offbytwo.jenkins.model.Build
import com.offbytwo.jenkins.model.BuildWithDetails
import com.offbytwo.jenkins.model.JobWithDetails
import com.offbytwo.jenkins.model.View
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import main.data.JenkinsServerInfo
import main.data.Job
import main.data.Plugin
import org.springframework.web.bind.annotation.*

@Api(description = "Jenkins REST API")
@RestController
@RequestMapping("/api/jenkins")
class JenkinsRestApi(
    private val jenkinsClient: JenkinsClient
) {
    @ApiOperation(value = "1.1 Получение информации о сервере Jenkins")
    @GetMapping("/info")
    fun getJenkinsServerInfo(): JenkinsServerInfo {
        return jenkinsClient.getJenkinsServerInfo()
    }

    @ApiOperation(value = "1.2 Получение списка Job")
    @GetMapping("/jobs")
    fun getAllJobs(): List<Job> {
        return jenkinsClient.getAllJobs()
    }

    @ApiOperation(value = "1.3 Получение информации о Job")
    @GetMapping("/job/{jobName}/info")
    fun getJobInfo(@PathVariable jobName: String): JobWithDetails {
        return jenkinsClient.getJobInfo(jobName)
    }

    @ApiOperation(value = "1.4 Запуск сборки Job")
    @PostMapping("/job/{jobName}/build")
    fun buildJob(@PathVariable jobName: String) {
        jenkinsClient.buildJob(jobName)
    }

    @ApiOperation(value = "1.5 Остановка сборки Job")
    @PostMapping("/job/{jobName}/stop/{buildNumber}")
    fun stopJobBuild(@PathVariable jobName: String, @PathVariable buildNumber: Int) {
        jenkinsClient.stopJobBuild(jobName, buildNumber)
    }

    @ApiOperation(value = "1.6 Получение списка сборок Job")
    @GetMapping("/job/{jobName}/builds")
    fun getJobBuilds(@PathVariable jobName: String): List<Build> {
        return jenkinsClient.getJobBuilds(jobName)
    }

    @ApiOperation(value = "1.7 Получение информации о сборке Job")
    @GetMapping("/job/{jobName}/build/{buildNumber}/info")
    fun getBuildInfo(@PathVariable jobName: String, @PathVariable buildNumber: Int): BuildWithDetails {
        return jenkinsClient.getBuildInfo(jobName, buildNumber)
    }

    @ApiOperation(value = "1.8 Получение консольного вывода сборки Job")
    @GetMapping("/job/{jobName}/build/{buildNumber}/console")
    fun getBuildConsoleOutput(@PathVariable jobName: String, @PathVariable buildNumber: Int): String {
        return jenkinsClient.getBuildConsoleOutput(jobName, buildNumber)
    }

    @ApiOperation(value = "1.9 Создание Job")
    @PostMapping("/job/create")
    fun createJob(@RequestParam jobName: String, @RequestParam jobConfig: String) {
        jenkinsClient.createJob(jobName, jobConfig)
    }

    @ApiOperation(value = "1.10 Копирование Job")
    @PostMapping("/job/copy")
    fun copyJob(@RequestParam sourceJobName: String, @RequestParam newJobName: String) {
        jenkinsClient.copyJob(sourceJobName, newJobName)
    }

    @ApiOperation(value = "1.11 Перемещение Job")
    @PostMapping("/job/move")
    fun moveJob(@RequestParam jobName: String, @RequestParam newFolder: String) {
        jenkinsClient.moveJob(jobName, newFolder)
    }

    @ApiOperation(value = "1.12 Переименование Job")
    @PostMapping("/job/rename")
    fun renameJob(@RequestParam oldJobName: String, @RequestParam newJobName: String) {
        jenkinsClient.renameJob(oldJobName, newJobName)
    }

    @ApiOperation(value = "1.13 Получение списка параметров Job")
    @GetMapping("/job/{jobName}/parameters")
    fun getJobParameters(@PathVariable jobName: String?): Map<String, String> {
        return jenkinsClient.getJobParameters(jobName)
    }

    @ApiOperation(value = "1.14 Получение значения параметра Job")
    @GetMapping("/job/{jobName}/parameter/{parameterName}")
    fun getJobParameterValue(@PathVariable jobName: String?, @PathVariable parameterName: String?): String? {
        return jenkinsClient.getJobParameterValue(jobName, parameterName)
    }

    @ApiOperation(value = "1.15 Изменение значения параметра Job")
    @PutMapping("/job/{jobName}/parameter/{parameterName}")
    fun setJobParameterValue(
        @PathVariable jobName: String?,
        @PathVariable parameterName: String,
        @RequestParam parameterValue: String
    ) {
        jenkinsClient.setJobParameterValue(jobName, parameterName, parameterValue)
    }

    @ApiOperation(value = "1.16. Удаление параметра Job")
    @DeleteMapping("/job/{jobName}/parameter/{parameterName}")
    fun deleteParameter(@PathVariable jobName: String, @PathVariable parameterName: String) {
        jenkinsClient.deleteParameter(jobName, parameterName)
    }

    @ApiOperation(value = "1.17 Создание нового параметра Job")
    @PostMapping("/job/{jobName}/parameter")
    fun createJobParameter(
        @PathVariable jobName: String,
        @RequestParam parameterName: String,
        @RequestParam defaultValue: String
    ) {
        jenkinsClient.createJobParameter(jobName, parameterName, defaultValue)
    }

    @ApiOperation(value = "1.18 Получение списка View")
    @GetMapping("/views")
    fun getViewList(): List<View> {
        return jenkinsClient.getViewList()
    }

    @ApiOperation(value = "1.19 Получение информации о View")
    @GetMapping("/view/{viewName}")
    fun getViewInfo(@PathVariable viewName: String): View {
        return jenkinsClient.getViewInfo(viewName)
    }

    @ApiOperation(value = "1.20 Создание новой View")
    @PostMapping("/view/create")
    fun createView(
        @RequestParam viewName: String,
        @RequestParam viewType: String,
        @RequestParam jobNames: List<String>
    ) {
        jenkinsClient.createView(viewName, viewType, jobNames)
    }

    @ApiOperation(value = "1.21 Редактирование View")
    @PutMapping("/view/{viewName}/update")
    fun updateView(
        @PathVariable viewName: String,
        @RequestParam viewXml: String,
        @RequestParam newJobNames: List<String>
    ) {
        jenkinsClient.updateView(viewName, viewXml, newJobNames)
    }

    @ApiOperation(value = "1.22 Удаление View")
    @DeleteMapping("/view/{viewName}/delete")
    fun deleteView(@PathVariable viewName: String) {
        jenkinsClient.deleteView(viewName)
    }

    @ApiOperation(value = "1.23 Получение списка доступных плагинов")
    @GetMapping("/plugins/available")
    fun getAvailablePlugins(): List<Plugin> {
        return jenkinsClient.getAvailablePlugins()
    }

    @ApiOperation(value = "1.24 Получение списка установленных плагинов")
    @GetMapping("/plugins/installed")
    fun getInstalledPlugins(): List<Plugin> {
        return jenkinsClient.getInstalledPlugins()
    }

    @ApiOperation(value = "1.25 Загрузка и установка плагина из Update Center")
    @PostMapping("/plugin/install")
    fun installPlugin(@RequestParam pluginId: String, @RequestParam version: String) {
        jenkinsClient.installPlugin(pluginId, version)
    }

    @ApiOperation(value = "1.26 Получение информации о плагине")
    @GetMapping("/plugin/{pluginId}/info")
    fun getPluginInfo(@PathVariable pluginId: String): Plugin {
        return jenkinsClient.getPluginInfo(pluginId)
    }

    @ApiOperation(value = "1.27 Включение плагина")
    @PostMapping("/plugin/{pluginId}/enable")
    fun enablePlugin(@PathVariable pluginId: String) {
        jenkinsClient.enablePlugin(pluginId)
    }

    @PostMapping("/plugin/disable/{pluginId}")
    @ApiOperation("1.28 Отключение плагина")
    fun disablePlugin(@PathVariable pluginId: String) {
        jenkinsClient.disablePlugin(pluginId)
    }

    @GetMapping("/groups")
    @ApiOperation("1.29 Получение списка групп")
    fun getAllGroups(): List<JenkinsClient.Group> {
        return jenkinsClient.getAllGroups()
    }

    @GetMapping("/group/{groupName}")
    @ApiOperation("1.30 Получение информации о группе")
    fun getGroupInfo(@PathVariable groupName: String): JenkinsClient.Group {
        return jenkinsClient.getGroupInfo(groupName)
    }

    @PostMapping("/group/create")
    @ApiOperation("1.31 Создание новой группы")
    fun createGroup(@RequestParam newGroupName: String) {
        jenkinsClient.createGroup(newGroupName)
    }

    @PostMapping("/group/delete")
    @ApiOperation("1.32 Удаление группы")
    fun deleteGroup(@RequestParam groupName: String) {
        jenkinsClient.deleteGroup(groupName)
    }

    @PostMapping("/user/{userName}/addToGroup/{groupName}")
    @ApiOperation("1.33 Добавление пользователя в группу")
    fun addUserToGroup(@PathVariable userName: String, @PathVariable groupName: String) {
        jenkinsClient.addUserToGroup(userName, groupName)
    }

    @PostMapping("/user/{userName}/removeFromGroup/{groupName}")
    @ApiOperation("1.34 Удаление пользователя из группы")
    fun removeUserFromGroup(@PathVariable userName: String, @PathVariable groupName: String) {
        jenkinsClient.removeUserFromGroup(userName, groupName)
    }

    @GetMapping("/group/{groupName}/permissions")
    @ApiOperation("1.35 Получение списка разрешений группы")
    fun getGroupPermissions(@PathVariable groupName: String): List<String> {
        return jenkinsClient.getGroupPermissions(groupName)
    }

    // 1.36 Добавление разрешения для группы - addGroupPermission
    @ApiOperation("1.36 Add group permission")
    @PostMapping("/groups/{groupName}/permissions/add")
    fun addGroupPermission(
        @PathVariable groupName: String,
        @RequestParam permissionId: String,
        @RequestParam(required = false) impliedBy: String?
    ) {
        jenkinsClient.addGroupPermission(groupName, permissionId, impliedBy)
    }

    // 1.37 Удаление разрешения для группы - removeGroupPermission
    @ApiOperation("1.37 Remove group permission")
    @PostMapping("/groups/{groupName}/permissions/remove")
    fun removeGroupPermission(
        @PathVariable groupName: String,
        @RequestParam permissionId: String,
        @RequestParam(required = false) impliedBy: String?
    ) {
        jenkinsClient.removeGroupPermission(groupName, permissionId, impliedBy)
    }

    // 1.38 Получение списка пользователей - getAllUsers
    @ApiOperation("1.38 Get all users")
    @GetMapping("/users")
    fun getAllUsers(): List<JenkinsClient.User> {
        return jenkinsClient.getAllUsers()
    }

    // 1.39 Получение информации о пользователе - getUserInfo
    @ApiOperation("1.39 Get user info")
    @GetMapping("/users/{userId}")
    fun getUserInfo(@PathVariable userId: String): JenkinsClient.UserInfo {
        return jenkinsClient.getUserInfo(userId)
    }

    // 1.40 Получение списка узлов (агентов) - getAllNodes
    @ApiOperation("1.40 Get all nodes")
    @GetMapping("/nodes")
    fun getAllNodes(): List<JenkinsClient.NodeInfo> {
        return jenkinsClient.getAllNodes()
    }

    // 1.41 Получение информации об узле (агенте) - getNodeInfo
    @ApiOperation("1.41 Get node info")
    @GetMapping("/nodes/{nodeName}")
    fun getNodeInfo(@PathVariable nodeName: String): JenkinsClient.NodeInfo {
        return jenkinsClient.getNodeInfo(nodeName)
    }

    // 1.42 Создание нового узла (агента) - createNode
    @ApiOperation("1.42 Create node")
    @PostMapping("/nodes")
    fun createNode(
        @RequestParam nodeName: String,
        @RequestBody nodeConfig: String
    ) {
        jenkinsClient.createNode(nodeName, nodeConfig)
    }

    @PostMapping("/node/{nodeName}/delete")
    @ApiOperation(value = "1.43 Удаление узла (агента)", notes = "Удаляет существующий узел (агент) Jenkins.")
    fun deleteNode(@PathVariable nodeName: String) {
        jenkinsClient.deleteNode(nodeName)
    }

    @PostMapping("/node/{nodeName}/enable")
    @ApiOperation(value = "1.44 Включение узла (агента)", notes = "Включает существующий узел (агент) Jenkins.")
    fun enableNode(@PathVariable nodeName: String) {
        jenkinsClient.enableNode(nodeName)
    }

    @PostMapping("/node/{nodeName}/disable")
    @ApiOperation(value = "1.45 Выключение узла (агента)", notes = "Отключает существующий узел (агент) Jenkins.")
    fun disableNode(@PathVariable nodeName: String) {
        jenkinsClient.disableNode(nodeName)
    }

    @PostMapping("/plugin/install")
    @ApiOperation(
        value = "1.46 Загрузка и установка плагина по ссылке",
        notes = "Загружает и устанавливает плагин Jenkins по указанной ссылке."
    )
    fun installPluginByUrl(@RequestParam pluginUrl: String) {
        jenkinsClient.installPluginByUrl(pluginUrl)
    }

    @GetMapping("/config")
    @ApiOperation(
        value = "1.47 Получение информации о текущем конфигурационном файле Jenkins",
        notes = "Возвращает текущий конфигурационный файл Jenkins в формате XML."
    )
    fun getCurrentConfigFile(): String {
        return jenkinsClient.getCurrentConfigFile()
    }

    @PutMapping("/config")
    @ApiOperation(
        value = "1.48 Обновление конфигурационного файла Jenkins",
        notes = "Обновляет конфигурационный файл Jenkins с использованием предоставленного XML-документа."
    )
    fun updateConfigFile(@RequestBody configXml: String) {
        jenkinsClient.updateConfigFile(configXml)
    }

    @GetMapping("/statistics")
    @ApiOperation(
        value = "1.49 Получение статистики о сервере Jenkins",
        notes = "Возвращает статистическую информацию о сервере Jenkins, такую как количество выполненных заданий, успешных и неуспешных сборок, и общее количество узлов."
    )
    fun getJenkinsStatistics(): JenkinsClient.JenkinsStatistics {
        return jenkinsClient.getJenkinsStatistics()
    }

    @PostMapping("/restart")
    @ApiOperation(value = "1.50 Запуск перезапуска сервера Jenkins", notes = "Перезапускает сервер Jenkins.")
    fun restartJenkins() {
        jenkinsClient.restartJenkins()
    }

}