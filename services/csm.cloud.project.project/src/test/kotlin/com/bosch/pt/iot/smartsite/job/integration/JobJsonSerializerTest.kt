/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.integration

import com.bosch.pt.iot.smartsite.project.workarea.config.WorkAreaIdOrEmptyJsonDeserializer
import com.bosch.pt.iot.smartsite.project.workarea.config.WorkAreaIdOrEmptyJsonSerializer
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.util.Locale
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.boot.jackson.JsonComponentModule

class JobJsonSerializerTest {

  private val jobJsonSerializer =
      JsonComponentModule()
          .apply {
            addSerializer(WorkAreaIdOrEmpty::class.java, WorkAreaIdOrEmptyJsonSerializer())
            addDeserializer(WorkAreaIdOrEmpty::class.java, WorkAreaIdOrEmptyJsonDeserializer())
          }
          .let { JobJsonSerializer(it, mapOf("TestCommandAlias" to TestCommand::class.java)) }

  @Test
  fun `serializes and deserializes objects (roundtrip)`() {
    val commandToSerialize = TestCommand(locale = Locale.UK, uuid = UUID.randomUUID())

    val result = jobJsonSerializer.serialize(commandToSerialize)

    assertThat(jobJsonSerializer.deserialize(result)).isEqualTo(commandToSerialize)
  }

  @Test
  fun `uses aliases as type information in serialized object`() {
    val commandToSerialize = TestCommand(locale = Locale.UK, uuid = UUID.randomUUID())

    val result = jobJsonSerializer.serialize(commandToSerialize)

    assertThat(result.type).isEqualTo("TestCommandAlias")
  }

  @Test
  fun `throws on unknown type on serialization`() {
    val commandToSerialize = UnknownCommand()

    assertThatIllegalStateException().isThrownBy { jobJsonSerializer.serialize(commandToSerialize) }
  }

  @Test
  fun `throws on unknown type on deserialization`() {
    val objectToDeserialize = JsonSerializedObject(type = "UnknownType", "{}")

    assertThatIllegalStateException().isThrownBy {
      jobJsonSerializer.deserialize(objectToDeserialize)
    }
  }
}

data class TestCommand(
    val locale: Locale = Locale.UK,
    val uuid: UUID = UUID.randomUUID(),
    val workAreaIdOrEmptyFilled: WorkAreaIdOrEmpty = WorkAreaIdOrEmpty(WorkAreaId()),
    val workAreaIdOrEmptyUnfilled: WorkAreaIdOrEmpty = WorkAreaIdOrEmpty(),
    val nullableCollection: Collection<String>? = null,
    val nullableField: String? = null
)

class UnknownCommand
