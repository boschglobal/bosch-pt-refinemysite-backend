package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(MessageSourceConfiguration::class)
class MessageSourceTest {

  @Autowired private lateinit var messageSource: MessageSource

  @Test
  fun loadCommonMessages() {
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, Locale.ENGLISH)).isNotBlank
    assertThat(messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, null, Locale.GERMAN)).isNotBlank
  }
}
