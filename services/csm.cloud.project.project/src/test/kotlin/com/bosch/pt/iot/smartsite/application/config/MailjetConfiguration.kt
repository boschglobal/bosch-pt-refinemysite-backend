/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import java.net.ServerSocket
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MailjetProperties::class)
open class MailjetConfiguration {

  @Bean open fun mailjetPort(): MailjetPort = MailjetPort.random()

  @Bean
  open fun mailjetClient(port: MailjetPort): MailjetClient {
    val clientOptions =
        ClientOptions.builder()
            .baseUrl("http://localhost:${port.value}/")
            .apiKey("key")
            .apiSecretKey("secret")
            .build()

    return MailjetClient(clientOptions)
  }
}

data class MailjetPort(val value: Int) {
  companion object {
    fun random(): MailjetPort = MailjetPort(getRandomFreePort())
    private fun getRandomFreePort(): Int = ServerSocket(0).use { it.localPort }
  }
}
