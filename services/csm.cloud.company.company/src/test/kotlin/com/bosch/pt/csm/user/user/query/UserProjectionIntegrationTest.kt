/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.user.query

import com.bosch.pt.csm.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.DE
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.valueOf
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedDate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getLastModifiedDate
import com.bosch.pt.csm.common.AbstractListenerIntegrationTest
import com.bosch.pt.csm.user.testdata.submitUserCreatedUnregistered
import com.bosch.pt.csm.common.util.getIdentifier
import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@SmartSiteSpringBootTest
class UserProjectionIntegrationTest : AbstractListenerIntegrationTest() {

  private val userIdentifier by lazy { getIdentifier("daniel").asUserId() }

  @Test
  fun `user created event is processed successfully`() {
    eventStreamGenerator.submitUser(asReference = "daniel")
    repositories.userProjectionRepository.findOneById(userIdentifier).also {
      validateUserAttributes(it!!, get("daniel")!!)
    }
  }

  @Test
  fun `user updated event is processed successfully`() {
    eventStreamGenerator.submitUser(asReference = "daniel")
    eventStreamGenerator.submitUser(asReference = "daniel", eventType = UPDATED) {
      it.email = "myNewEmail@adress.com"
    }

    repositories.userProjectionRepository.findOneById(userIdentifier).also {
      validateUserAttributes(it!!, get("daniel")!!)
    }
  }

  @Test
  fun `user updated event with different locale and country is processed successfully`() {
    eventStreamGenerator.submitUser(asReference = "daniel") {
      it.locale = "en_GB"
      it.country = null
    }

    repositories.userProjectionRepository.findOneById(userIdentifier).also {
      assertThat(it).isNotNull
      assertThat(it!!.locale.toString()).isEqualTo("en_GB")
      assertThat(it.country).isNull()
    }

    eventStreamGenerator.submitUser(asReference = "daniel", eventType = UPDATED) {
      it.locale = null
      it.country = DE
    }

    repositories.userProjectionRepository.findOneById(userIdentifier).also {
      assertThat(it).isNotNull
      assertThat(it!!.locale).isNull()
      assertThat(it.country).isEqualTo(valueOf(DE.name))
    }
  }

  @Test
  fun `user registered event is processed successfully`() {
    eventStreamGenerator.submitUserCreatedUnregistered("someoneElse")

    repositories.userProjectionRepository
        .findOneById(getIdentifier("someoneElse").asUserId())
        .also { assertThat(it).isNull() }

    eventStreamGenerator.submitUser(asReference = "someoneElse", eventType = REGISTERED) {
      it.firstName = "Someone"
      it.lastName = "Else"
      it.registered = true
    }

    repositories.userProjectionRepository
        .findOneById(getIdentifier("someoneElse").asUserId())
        .also { validateUserAttributes(it!!, get("someoneElse")!!) }
  }

  @Test
  fun `user deleted event is processed successfully`() {
    eventStreamGenerator.submitUser("daniel").submitUserTombstones("daniel")

    val userAggregate = get<UserAggregateAvro>("daniel")!!
    val user =
        repositories.userProjectionRepository.findOneById(userAggregate.getIdentifier().asUserId())

    assertThat(user).isNull()
  }

  private fun validateUserAttributes(user: UserProjection, userAggregate: UserAggregateAvro) {
    assertThat(user).isNotNull
    assertThat(user.id).isEqualTo(userAggregate.getIdentifier().asUserId())
    assertThat(user.createdBy).isEqualTo(userAggregate.getCreatedByUserIdentifier().asUserId())
    assertThat(user.createdDate.toInstant()).isEqualTo(userAggregate.getCreatedDate())
    assertThat(user.lastModifiedBy)
        .isEqualTo(userAggregate.getLastModifiedByUserIdentifier().asUserId())
    assertThat(user.lastModifiedDate.toInstant()).isEqualTo(userAggregate.getLastModifiedDate())
    assertThat(user.version).isEqualTo(userAggregate.aggregateIdentifier.version)
    assertThat(user.ciamUserIdentifier).isEqualTo(userAggregate.userId)
    assertThat(user.firstName).isEqualTo(userAggregate.firstName)
    assertThat(user.lastName).isEqualTo(userAggregate.lastName)
    assertThat(user.admin).isEqualTo(userAggregate.admin)
    assertThat(user.email).isEqualTo(userAggregate.email)
    assertThat(user.locked).isEqualTo(userAggregate.locked)

    if (userAggregate.locale == null) {
      assertThat(user.locale).isNull()
    } else {
      assertThat(user.locale).isEqualTo(LocaleUtils.toLocale(userAggregate.locale))
    }
  }
}
