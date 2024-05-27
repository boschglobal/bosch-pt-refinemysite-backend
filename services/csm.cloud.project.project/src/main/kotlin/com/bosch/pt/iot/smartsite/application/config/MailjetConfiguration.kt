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
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MailjetProperties::class)
open class MailjetConfiguration(private val mailjetProperties: MailjetProperties) {

  @Bean
  open fun mailjetClient(): MailjetClient {
    if (!mailjetProperties.enabled) {
      LOGGER.warn("Mailjet is disabled. Mails will not be sent.")
    }
    if (mailjetProperties.sandboxMode) {
      // Note: the sandbox mode is not a global mailjet option. It needs to be set when sending an
      // email. Still we log the warning here to not warn repeatedly for every sent mail.
      LOGGER.warn(
          "Mailjet sandbox mode is active. Mailjet will validate requests but not actually send mails.")
    }

    val clientOptions =
        mailjetProperties.api?.let {
          ClientOptions.builder().apiKey(it.key).apiSecretKey(it.secret).build()
        }
            ?: throw IllegalArgumentException("Mailjet API credentials could not be found.")

    return MailjetClient(clientOptions)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(MailjetConfiguration::class.java)
  }
}
