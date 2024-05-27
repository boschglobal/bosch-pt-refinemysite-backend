/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import com.bosch.pt.csm.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@SmartSiteSpringBootTest
class MessageSourceTest {

  @Autowired private lateinit var messageSource: MessageSource

  @Test
  fun loadLocalMessages() {
    assertThat(
            messageSource.getMessage(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED, null, Locale.ENGLISH))
        .isNotBlank
    assertThat(
            messageSource.getMessage(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED, null, Locale.GERMAN))
        .isNotBlank
  }

  @Test
  fun loadCommonMessages() {
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, Locale.ENGLISH)).isNotBlank
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, Locale.GERMAN)).isNotBlank
  }
}
