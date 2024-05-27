/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.eventstore.KafkaAvroTestSerializer
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.mapper.UserAvroSnapshotMapper.toAvroMessageWithNewVersion
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.mapper.UserAvroSnapshotMapper.toMessageKeyWithNewVersion
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.asValueObject
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserBuilder.defaultUser
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.Optional
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.core.env.Environment
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils

@SmartSiteMockKTest
class UserContextEventStoreBlockModifyingOperationsTest {

  @MockK(relaxed = true) private lateinit var entityManager: EntityManager

  @MockK(relaxed = true) private lateinit var kafkaProperties: KafkaProperties

  @MockK(relaxed = true) private lateinit var kafkaTopicProperties: KafkaTopicProperties

  @MockK(relaxed = true) private lateinit var environment: Environment

  private lateinit var cut: UserContextEventStore

  @BeforeEach
  fun setup() {
    every { kafkaProperties.properties } returns
        mapOf("schema.registry.url" to "mock://", "specific.avro.reader" to "true")
    cut =
        UserContextEventStore(
            kafkaTopicProperties,
            false,
            entityManager,
            kafkaProperties,
            environment,
            Optional.empty())
  }

  @Test
  fun verifyBlocking() {
    // Enable blocking
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", true)

    // set a fake authentication context
    setAuthentication()

    // Ensure that message cannot be sent
    assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
      buildUser().asValueObject().apply {
        cut.save(toMessageKeyWithNewVersion(this), toAvroMessageWithNewVersion(this, CREATED))
      }
    }

    // Check mocks
    verify { listOf(entityManager, kafkaTopicProperties) wasNot called }
  }

  @Test
  fun verifyNotBlocking() {

    // Disable blocking and set serializer
    ReflectionTestUtils.setField(cut, "blockModifyingOperations", false)
    ReflectionTestUtils.setField(cut, "kafkaAvroSerializer", KafkaAvroTestSerializer())

    // set a fake authentication context
    setAuthentication()

    // Mock kafka properties beans
    mockKafkaProperties()

    // Send message
    buildUser().asValueObject().apply {
      cut.save(toMessageKeyWithNewVersion(this), toAvroMessageWithNewVersion(this, CREATED))
    }

    // Check mocks
    verify { entityManager.persist(any()) }
    confirmVerified(entityManager)
  }

  private fun buildUser(): User =
      defaultUser().apply {
        setCreatedBy(this.identifier)
        setLastModifiedBy(this.identifier)
        version = 0L
      }

  private fun mockKafkaProperties() {
    every { kafkaTopicProperties.getConfigForChannel(any()) } returns
        KafkaTopicProperties.TopicConfig()
    every { kafkaTopicProperties.getTopicForChannel(any()) } returns "a"
  }

  private fun setAuthentication() {
    TestSecurityContextHolder.setAuthentication(
        UsernamePasswordAuthenticationToken(
            User().apply { this.identifier = UserId() },
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_USER")))
  }
}
