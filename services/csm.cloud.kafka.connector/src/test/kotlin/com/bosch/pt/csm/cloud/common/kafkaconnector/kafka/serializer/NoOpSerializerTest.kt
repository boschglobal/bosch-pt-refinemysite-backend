/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.kafka.serializer

import java.nio.charset.StandardCharsets.UTF_8
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class NoOpSerializerTest {

  private val cut = NoOpSerializer()

  @Test
  fun verifyDataNull() {
    assertThat(cut.serialize(TOPIC, null)).isNull()
  }

  @Test
  fun verifySerializeByteArray() {
    val bytes = "test".toByteArray(UTF_8)
    assertThat(cut.serialize(TOPIC, bytes)).isEqualTo(bytes)
  }

  @Test
  fun verifyIfNotNullOrByteArray() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      cut.serialize(TOPIC, "test")
    }
  }

  companion object {
    private const val TOPIC = "fake-topic"
  }
}
