/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.extensions.toUserId
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatUpdated
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.asPatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.request.CreateOrUpdatePatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt

class PatApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var patEventStoreUtils: EventStoreUtils<PatKafkaEvent>

  @Autowired private lateinit var cut: CurrentUserPatController

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUserAndActivate("user")
    setAuthentication("user")
  }

  @Test
  fun `create PAT and verify event`() {
    val principal = SecurityContextHolder.getContext().authentication.principal as User

    val patCreatedResource =
        requireNotNull(
            cut.createPat(
                    CreateOrUpdatePatResource(
                        description = "This is my PAT",
                        scopes = listOf(PatScopeEnum.GRAPHQL_API_READ),
                        validForMinutes = 60),
                    principal)
                .body)

    val user = get<UserAggregateAvro>("user")!!
    val event = patEventStreamUtils.verifyContainsAndGet(PatCreatedEventAvro::class.java, null)

    assertEquals(patCreatedResource.description, event.description)
    assertEquals(listOf(PatScopeEnumAvro.GRAPHQL_API_READ), event.scopes)
    assertEquals(PatTypeEnumAvro.RMSPAT1, event.type)
    assertEquals(user.toUserId().toString(), event.impersonatedUser.identifier)
    assertEquals(patCreatedResource.id.toString(), event.aggregateIdentifier.identifier)
    assertEquals((patCreatedResource.version), event.aggregateIdentifier.version)
  }

  @Test
  fun `update PAT and verify event`() {
    val principal = SecurityContextHolder.getContext().authentication.principal as User

    val pat = eventStreamGenerator.submitPatCreated("PAT", "user").get<PatCreatedEventAvro>("PAT")!!

    val updatePatResource =
        CreateOrUpdatePatResource(
            description = "This is my updated PAT",
            scopes = listOf(PatScopeEnum.GRAPHQL_API_READ, PatScopeEnum.TIMELINE_API_READ),
            validForMinutes = 24 * 60 * 3)

    val patUpdatedResource =
        requireNotNull(
            cut.updatePat(
                    updatePatResource,
                    PatId(pat.aggregateIdentifier.identifier),
                    principal,
                    ETag.from("0"))
                .body)

    val event = patEventStreamUtils.verifyContainsAndGet(PatUpdatedEventAvro::class.java, null)

    assertEquals(updatePatResource.description, event.description)
    assertEquals(
        listOf(PatScopeEnumAvro.GRAPHQL_API_READ, PatScopeEnumAvro.TIMELINE_API_READ), event.scopes)
    assertEquals(patUpdatedResource.expiresAt.time, event.expiresAt)

    assertEquals(patUpdatedResource.id.toString(), event.aggregateIdentifier.identifier)
    assertEquals((patUpdatedResource.version), event.aggregateIdentifier.version)
  }

  @Test
  fun `create PAT and verify hash check`() {

    val principal = SecurityContextHolder.getContext().authentication.principal as User

    val patCreatedResource =
        cut.createPat(
                CreateOrUpdatePatResource(
                    description = "This is my PAT",
                    scopes = listOf(PatScopeEnum.GRAPHQL_API_READ),
                    validForMinutes = 60),
                principal)
            .body

    val pat =
        requireNotNull(
            repositories.patRepository.findByIdentifier(patCreatedResource!!.id.asPatId()))

    assertEquals(principal.identifier.toUuid(), pat.impersonatedUser.identifier)

    // Verifies the plain token value can be verified against the (stored) hash using
    // BCrypt.checkpw()
    assertTrue(BCrypt.checkpw(patCreatedResource.token, pat.hash))
  }

  @Test
  fun `deleting a user creates tombstone messages for every version of the user`() {

    val principal = SecurityContextHolder.getContext().authentication.principal as User

    eventStreamGenerator
        .submitPatCreated("PAT", "user")
        .submitPatUpdated("PAT", "user") { it.description = "First update to PAT" }
        .submitPatUpdated("PAT", "user") { it.description = "Second update to PAT" }

    patEventStoreUtils.reset()

    cut.deletePat(getIdentifier("PAT").asPatId(), principal, ETag.from("2"))

    patEventStoreUtils.verifyContainsTombstoneMessageAndGet(3).also {
      validateTombstoneMessageKey(it[0], "PAT", 0)
      validateTombstoneMessageKey(it[1], "PAT", 1)
      validateTombstoneMessageKey(it[2], "PAT", 2)
    }
  }

  @Test
  fun `verify delete unknown user throws exception`() {
    val principal = SecurityContextHolder.getContext().authentication.principal as User
    Assertions.assertThatThrownBy {
          cut.deletePat(PatId(UUID.randomUUID()), principal, ETag.from("1"))
        }
        .isInstanceOf(AggregateNotFoundException::class.java)
        .hasMessage(Key.PAT_VALIDATION_ERROR_NOT_FOUND)
  }

  @Suppress("SameParameterValue")
  private fun validateTombstoneMessageKey(
      messageKey: MessageKeyAvro,
      reference: String,
      version: Long,
  ) {
    getByReference(reference)
        .let {
          AggregateIdentifierAvro.newBuilder()
              .setType(it.type)
              .setVersion(version)
              .setIdentifier(it.identifier)
              .build()
        }
        .apply { Assertions.assertThat(messageKey.aggregateIdentifier).isEqualTo(this) }
  }
}
