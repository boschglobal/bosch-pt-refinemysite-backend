/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import java.util.Locale.ENGLISH
import java.util.Locale.GERMAN
import java.util.TimeZone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@SmartSiteSpringBootTest
class MessageSourceTest {

  @Autowired private lateinit var messageSource: MessageSource

  @Test
  fun loadLocalMessages() {
    assertThat(messageSource.getMessage(TASK_VALIDATION_ERROR_NOT_FOUND, null, ENGLISH)).isNotBlank
    assertThat(messageSource.getMessage(TASK_VALIDATION_ERROR_NOT_FOUND, null, GERMAN)).isNotBlank
  }

  @Test
  fun loadCommonMessages() {
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, ENGLISH)).isNotBlank
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, GERMAN)).isNotBlank
  }

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}
