/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatDeleted
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatUpdated
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro.RMSPAT1
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class PatProjectorEventListenerIntegrationTest : AbstractIntegrationTest() {

  @Test
  fun `pat is created`() {
    val userAggregate = eventStreamGenerator.submitUser("u1").get<UserAggregateAvro>("u1")!!
    val patEvent =
        eventStreamGenerator
            .submitPatCreated("pat1", "u1") {
              it.description = "pat1"
              it.hash = "<secure-hash>"
              it.scopes = listOf(GRAPHQL_API_READ)
              it.type = RMSPAT1
            }
            .get<PatCreatedEventAvro>("pat1")!!

    val pat = findPatProjection("pat1")!!

    assertThat(pat.description).isEqualTo(patEvent.description)
    assertThat(pat.eventDate).isEqualTo(patEvent.auditingInformation.date.toLocalDateTimeByMillis())
    assertThat(pat.expiresAt).isEqualTo(patEvent.expiresAt.toLocalDateTimeByMillis())
    assertThat(pat.hash).isEqualTo(patEvent.hash)
    assertThat(pat.identifier.value.toString()).isEqualTo(patEvent.aggregateIdentifier.identifier)
    assertThat(pat.impersonatedUserIdentifier.value.toString())
        .isEqualTo(patEvent.impersonatedUser.identifier)
    assertThat(pat.issuedAt).isEqualTo(patEvent.issuedAt.toLocalDateTimeByMillis())
    assertThat(pat.scopes).isEqualTo(patEvent.scopes.map { PatScopeEnum.valueOf(it.name) })
    assertThat(pat.type).isEqualTo(PatTypeEnum.valueOf(patEvent.type.name))
    assertThat(pat.version).isEqualTo(patEvent.aggregateIdentifier.version)
    assertThat(pat.locale?.toString()).isEqualTo(userAggregate.locale)
    assertThat(pat.locked).isEqualTo(userAggregate.locked)
  }

  @Test
  fun `pat is updated event`() {
    val userAggregate = eventStreamGenerator.submitUser("u1").get<UserAggregateAvro>("u1")!!
    val patCreatedEvent =
        eventStreamGenerator
            .submitPatCreated("pat1", "u1") {
              it.description = "pat1"
              it.hash = "<secure-hash>"
              it.scopes = listOf(GRAPHQL_API_READ)
              it.type = RMSPAT1
            }
            .get<PatCreatedEventAvro>("pat1")!!
    val patUpdatedEvent =
        eventStreamGenerator
            .submitPatUpdated("pat1", "u1") {
              it.description = "updated pat1"
              it.expiresAt = LocalDate.now().plusDays(10).toEpochMilli()
              it.scopes = listOf(TIMELINE_API_READ)
            }
            .get<PatUpdatedEventAvro>("pat1")!!

    val pat = findPatProjection("pat1")!!

    assertThat(pat.hash).isEqualTo(patCreatedEvent.hash)
    assertThat(pat.impersonatedUserIdentifier.value.toString())
        .isEqualTo(patCreatedEvent.impersonatedUser.identifier)
    assertThat(pat.issuedAt).isEqualTo(patCreatedEvent.issuedAt.toLocalDateTimeByMillis())
    assertThat(pat.type).isEqualTo(PatTypeEnum.valueOf(patCreatedEvent.type.name))

    assertThat(pat.description).isEqualTo(patUpdatedEvent.description)
    assertThat(pat.eventDate)
        .isEqualTo(patUpdatedEvent.auditingInformation.date.toLocalDateTimeByMillis())
    assertThat(pat.expiresAt).isEqualTo(patUpdatedEvent.expiresAt.toLocalDateTimeByMillis())
    assertThat(pat.identifier.value.toString())
        .isEqualTo(patUpdatedEvent.aggregateIdentifier.identifier)
    assertThat(pat.scopes).isEqualTo(patUpdatedEvent.scopes.map { PatScopeEnum.valueOf(it.name) })
    assertThat(pat.version).isEqualTo(patUpdatedEvent.aggregateIdentifier.version)

    assertThat(pat.locale?.toString()).isEqualTo(userAggregate.locale)
    assertThat(pat.locked).isEqualTo(userAggregate.locked)
  }

  @Test
  fun `pat is removed`() {
    eventStreamGenerator.submitUser("u1").get<UserAggregateAvro>("u1")!!
    eventStreamGenerator
        .submitPatCreated("pat1", "u1") {
          it.description = "pat1"
          it.hash = "<secure-hash>"
          it.scopes = listOf(GRAPHQL_API_READ)
          it.type = RMSPAT1
        }
        .submitPatUpdated("pat1", "u1") { it.scopes = listOf(TIMELINE_API_READ) }

    assertThat(findPatProjection("pat1")).isNotNull

    eventStreamGenerator.submitPatDeleted("pat1")

    assertThat(findPatProjection("pat1")).isNull()
  }

  @Test
  fun `pat is created for a locked user`() {
    eventStreamGenerator.submitUser("u1").submitUser("u1", eventType = UPDATED) { it.locked = true }
    eventStreamGenerator.submitPatCreated("pat1", "u1") {
      it.description = "pat1"
      it.hash = "<secure-hash>"
      it.scopes = listOf(GRAPHQL_API_READ)
      it.type = RMSPAT1
    }

    val pat = findPatProjection("pat1")!!

    assertThat(pat.locked).isTrue()
  }

  @Test
  fun `pat is updated when a user is locked`() {
    eventStreamGenerator.submitUser("u1")
    eventStreamGenerator.submitPatCreated("pat1", "u1") {
      it.description = "pat1"
      it.hash = "<secure-hash>"
      it.scopes = listOf(GRAPHQL_API_READ)
      it.type = RMSPAT1
    }
    eventStreamGenerator.submitUser("u1") { it.locked = true }

    val pat = findPatProjection("pat1")!!

    assertThat(pat.locked).isTrue()
  }

  @Test
  fun `pat is updated when a user is unlocked`() {
    eventStreamGenerator.submitUser("u1") { it.locked = true }
    eventStreamGenerator.submitPatCreated("pat1", "u1") {
      it.description = "pat1"
      it.hash = "<secure-hash>"
      it.scopes = listOf(GRAPHQL_API_READ)
      it.type = RMSPAT1
    }

    var pat = findPatProjection("pat1")!!

    assertThat(pat.locked).isTrue()

    eventStreamGenerator.submitUser("u1") { it.locked = false }

    pat = findPatProjection("pat1")!!

    assertThat(pat.locked).isFalse()
  }

  private fun findPatProjection(reference: String) =
      repositories.patRepository.findOneByIdentifier(getIdentifier(reference).asPatId())
}
