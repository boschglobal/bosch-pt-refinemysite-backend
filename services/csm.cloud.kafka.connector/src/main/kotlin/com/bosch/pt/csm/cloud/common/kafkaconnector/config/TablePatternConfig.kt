/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.config

import java.util.regex.Pattern
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TablePatternConfig {
  @Bean
  fun tablePattern(@Value("\${event.table-pattern}") tablePattern: String): Pattern =
      Pattern.compile(tablePattern)
}
