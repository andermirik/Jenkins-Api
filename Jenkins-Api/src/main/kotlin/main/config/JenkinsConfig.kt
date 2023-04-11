package main.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.client.JenkinsHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
open class JenkinsConfig(
    @Value("jenkins.uri")
    private val uri: String,
    @Value("jenkins.username")
    private val username: String,
    @Value("jenkins.password")
    private val password: String
) {
    @Bean
    open fun jenkinsServer(): JenkinsServer {
        return JenkinsServer(URI(uri), username, password)
    }

    @Bean
    open fun jenkinsHttpClient(): JenkinsHttpClient {
        return JenkinsHttpClient(URI(uri), username, password)
    }

    @Bean
    open fun objectMapper(): ObjectMapper {
        return ObjectMapper().registerModule(KotlinModule())
    }
}