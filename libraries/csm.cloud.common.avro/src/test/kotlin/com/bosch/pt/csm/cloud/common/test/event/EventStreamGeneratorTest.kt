/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.event

import com.bosch.pt.csm.cloud.common.messages.PrimitiveFieldTestAvro
import com.bosch.pt.csm.cloud.common.messages.PrimitiveOptionalFieldTestAvro
import com.bosch.pt.csm.cloud.common.messages.TestCreatedEventAvro
import com.bosch.pt.csm.cloud.common.messages.TestUpdatedEventAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.DefaultTimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.EventConsumerRecord
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

class EventStreamGeneratorTest {

  private val eventStreamGenerator = EventStreamGenerator(TestEventStreamContext())

  @AfterEach
  fun clear() {
    eventStreamGenerator.reset()
  }

  @Test
  fun `primitive resolved`() {
    sendEvents(primitive = 1L)

    val primitive: Long = eventStreamGenerator.getEventField("event", "primitive")
    assertThat(primitive).isEqualTo(1L)
  }

  @Test
  fun `primitive optional resolved`() {
    sendEvents(primitiveOptional = "expectedPrimitiveOptional")

    val primitiveOptional: String = eventStreamGenerator.getEventField("event", "primitiveOptional")
    assertThat(primitiveOptional).isEqualTo("expectedPrimitiveOptional")
  }

  @Test
  fun `primitive optional null`() {
    sendEvents(primitiveOptional = null)

    val primitiveOptional: String? =
        eventStreamGenerator.getEventFieldOrNull("event", "primitiveOptional")
    assertThat(primitiveOptional).isNull()
  }

  @Test
  fun `nested primitive resolved`() {
    sendEvents(nestedPrimitive = "expectedNestedPrimitive")

    val nestedPrimitive: String =
        eventStreamGenerator.getEventField("event", "nestedPrimitive", "value")
    assertThat(nestedPrimitive).isEqualTo("expectedNestedPrimitive")
  }

  @Test
  fun `nested primitive optional resolved`() {
    sendEvents(nestedPrimitiveOptional = "expectedNestedPrimitiveOptional")

    val nestedPrimitiveOptional: String =
        eventStreamGenerator.getEventField("event", "nestedPrimitiveOptional", "value")
    assertThat(nestedPrimitiveOptional).isEqualTo("expectedNestedPrimitiveOptional")
  }

  @Test
  fun `nested primitive optional null resolved`() {
    sendEvents(nestedPrimitiveOptional = null)

    val nestedPrimitiveOptional: String? =
        eventStreamGenerator.getEventFieldOrNull("event", "nestedPrimitiveOptional", "value")
    assertThat(nestedPrimitiveOptional).isNull()
  }

  @Test
  fun `primitive not shared resolved`() {
    sendEvents(primitiveNotShared = 2L)

    val primitiveNotShared: Long = eventStreamGenerator.getEventField("event", "primitiveNotShared")
    assertThat(primitiveNotShared).isEqualTo(2L)
  }

  @Test
  fun `primitive shared resolved`() {
    sendEvents()

    val primitiveShared: String = eventStreamGenerator.getEventField("event", "primitiveShared")
    assertThat(primitiveShared).isEqualTo("updated")
  }

  private fun sendEvents(
      primitive: Long = 1,
      primitiveOptional: String? = null,
      primitiveNotShared: Long = 2,
      nestedPrimitive: String = "default",
      nestedPrimitiveOptional: String? = null,
  ) {
    val identifier = randomUUID()
    val rootContextIdentifier = randomUUID()
    val messageKey1 = messageKey(identifier, rootContextIdentifier, 0L)
    val messageKey2 = messageKey(identifier, rootContextIdentifier, 1L)

    val createdEvent =
        createdEvent(
            primitive = primitive,
            primitiveOptional = primitiveOptional,
            nestedPrimitive = nestedPrimitive,
            nestedPrimitiveOptional = nestedPrimitiveOptional)
    val updatedEvent = updatedEvent(primitiveNotShared)

    eventStreamGenerator.send("test", "event", messageKey1, createdEvent, 0)
    eventStreamGenerator.send("test", "event", messageKey2, updatedEvent, 1)
  }

  private fun updatedEvent(primitiveNotShared: Long): TestUpdatedEventAvro =
      TestUpdatedEventAvro.newBuilder()
          .setPrimitiveShared("updated")
          .setPrimitiveNotShared(primitiveNotShared)
          .build()

  private fun createdEvent(
      primitive: Long,
      primitiveOptional: String? = null,
      nestedPrimitive: String,
      nestedPrimitiveOptional: String? = null
  ): TestCreatedEventAvro =
      TestCreatedEventAvro.newBuilder()
          .setPrimitiveShared("created")
          .setPrimitive(primitive)
          .setPrimitiveOptional(primitiveOptional)
          .setNestedPrimitive(PrimitiveFieldTestAvro.newBuilder().setValue(nestedPrimitive).build())
          .setNestedPrimitiveOptional(
              PrimitiveOptionalFieldTestAvro.newBuilder().setValue(nestedPrimitiveOptional).build())
          .build()

  private fun messageKey(identifier: UUID, rootContextIdentifier: UUID, version: Long) =
      AggregateEventMessageKey(
          AggregateIdentifier("type", identifier, version), rootContextIdentifier)

  class TestEventStreamContext :
      EventStreamContext(
          mutableMapOf(),
          mutableMapOf(),
          DefaultTimeLineGenerator(),
          mutableMapOf("test" to listOf(::listen)),
          mutableMapOf()) {

    override fun send(runnable: Runnable) {
      runnable.run()
    }

    companion object {

      @Suppress("UNUSED_PARAMETER")
      private fun listen(record: EventConsumerRecord, ack: Acknowledgment) {
        ack.acknowledge()
      }
    }
  }
}
