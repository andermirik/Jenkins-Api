package main.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Plugin(
    val shortName: String? = null,
    val displayName: String? = null,
    val url: String? = null,
    val version: String? = null,
    val enabled: Boolean? = null,
    val active: Boolean? = null,
    val bundled: Boolean? = null,
    val pinned: Boolean? = null,
    val supportsDynamicLoad: String? = null,
    val longName: String? = null,
    val author: String? = null,
    val description: String? = null
)

data class JenkinsServerInfo(
    val mode: String? = null,
    val nodeName: String? = null,
    val numExecutors: Int? = null,
    val description: String? = null,
    val jobs: List<Job>? = null,
    val views: List<View>? = null,
    val quietingDown: Boolean? = null,
    val useCrumbs: Boolean? = null,
    val useSecurity: Boolean? = null,
    val nodeDescription: String? = null,
    val primaryView: View? = null,
    val slaveAgentPort: Int? = null,
    val overallLoad: LoadStatistics? = null,
    val unlabeledLoad: LoadStatistics? = null
)

data class Job(
    val name: String? = null,
    val url: String? = null,
    val color: String? = null,
    val _class: String? = null
)

data class View(
    val name: String? = null,
    val url: String? = null,
    val _class: String? = null
)

data class LoadStatistics(
    val busyExecutors: Int? = null,
    val totalExecutors: Int? = null,
    val queueLength: Int? = null,
    val availableExecutors: Int? = null,
    val connectingExecutors: Int? = null,
    val idleExecutors: Int? = null,
    val offlineExecutors: Int? = null
)