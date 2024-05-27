/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.kafka.eventstore

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.common.kafka.serializer.KafkaAvroTestSerializer
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPictureBuilder.Companion.projectPicture
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import jakarta.persistence.EntityManager
import java.util.Optional
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.test.util.ReflectionTestUtils

@SmartSiteMockKTest
class EventStoreBlockModifyingOperationsTest {

  @RelaxedMockK private lateinit var entityManager: EntityManager

  @RelaxedMockK private lateinit var kafkaProperties: KafkaProperties

  @RelaxedMockK private lateinit var kafkaTopicProperties: KafkaTopicProperties

  @RelaxedMockK private lateinit var environment: Environment

  private lateinit var cut: EventStoreImpl

  @BeforeEach
  fun setup() {
    every { kafkaProperties.properties } returns
        mapOf("schema.registry.url" to "mock://", "specific.avro.reader" to "true")

    cut =
        EventStoreImpl(
            entityManager, kafkaProperties, kafkaTopicProperties, environment, Optional.empty())
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun verifyBlocking() {

    // Enable blocking
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", true)
    val projectPicture = projectPicture().build()

    // Ensure that message cannot be sent
    assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
      cut.save(projectPicture as AbstractKafkaStreamable<*, *, Enum<*>>)
    }

    // Check mocks
    confirmVerified(entityManager, kafkaTopicProperties)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun verifyNotBlocking() {

    // Disable blocking and set serializer
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", false)
    ReflectionTestUtils.setField(cut, "kafkaAvroSerializer", KafkaAvroTestSerializer())

    // Mock kafka properties beans
    mockKafkaProperties()

    // Generate the message to send
    val projectPicture = buildProjectPicture()

    // Send message
    cut.save(projectPicture as AbstractKafkaStreamable<*, *, Enum<*>>)

    // Check mocks
    verify { entityManager.persist(any()) }
    confirmVerified(entityManager)
  }

  private fun buildProjectPicture(): ProjectPicture {
    val user = user().build()
    val projectPicture = projectPicture().withCreatedBy(user).withLastModifiedBy(user).build()
    projectPicture.eventType = ProjectPictureEventEnumAvro.CREATED
    ReflectionTestUtils.setField(user, "version", 1L)
    ReflectionTestUtils.setField(projectPicture, "version", 1L)
    return projectPicture
  }

  private fun mockKafkaProperties() {
    every { kafkaTopicProperties.getConfigForChannel(any()) } returns
        KafkaTopicProperties.TopicConfig()
    every { kafkaTopicProperties.getTopicForChannel(any()) } returns "a"
  }
}
