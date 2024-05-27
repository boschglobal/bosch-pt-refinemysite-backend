package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
class UserContextEventStoreIntegrationTest : AbstractIntegrationTest() {

  @Autowired private lateinit var cut: UserContextEventStore

  @Test
  fun `creating event exceeding 900 KB throws exception`() {
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { cut.createKafkaEvent("", "", 0, ByteArray(10), ByteArray(900 * 1_024)) }
        .withMessageContaining("must not exceed 921600 bytes")
  }

  @Test
  fun `creating event not exceeding 900 KB succeeds`() {
    assertThatCode { cut.createKafkaEvent("", "", 0, ByteArray(0), ByteArray(900 * 1_024)) }
        .doesNotThrowAnyException()
  }
}
