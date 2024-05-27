/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.extension.asNumberType
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class UserRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: UserAggregateAvro

  lateinit var aggregateV1: UserAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query user`() {
    submitEvents()

    // Execute query
    val userList = query()

    // Validate payload
    assertThat(userList.users).hasSize(1)
    val userV1 = userList.users.first()
    assertThat(aggregateV1.phoneNumbers).hasSize(1)
    val phoneNumberV1 = aggregateV1.phoneNumbers[0]

    assertThat(userV1.id).isEqualTo(aggregateV1.getIdentifier().asUserId())
    assertThat(userV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(userV1.firstName).isEqualTo(aggregateV1.firstName)
    assertThat(userV1.lastName).isEqualTo(aggregateV1.lastName)
    assertThat(userV1.email).isEqualTo(aggregateV1.email)
    assertThat(userV1.position).isEqualTo(aggregateV1.position)
    assertThat(userV1.locale?.toString()).isEqualTo(aggregateV1.locale)
    assertThat(userV1.country).isEqualTo(IsoCountryCodeEnum.valueOf(aggregateV1.country.name))
    assertThat(userV1.phoneNumbers).hasSize(1)
    assertThat(userV1.phoneNumbers[0].callNumber).isEqualTo(phoneNumberV1.callNumber)
    assertThat(userV1.phoneNumbers[0].countryCode).isEqualTo(phoneNumberV1.countryCode)
    assertThat(userV1.phoneNumbers[0].phoneNumberType)
        .isEqualTo(phoneNumberV1.phoneNumberType.asNumberType().key)
    assertThat(userV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
  }

  @Test
  fun `query deleted user`() {
    submitAsDeletedEvents()

    // Execute query
    val userList = query()

    // Validate payload
    assertThat(userList.users).isEmpty()
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("csm-user")!!
    aggregateV1 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
              it.lastName = "Changed"
            }
            .get("csm-user")!!
  }

  private fun submitAsDeletedEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("csm-user")!!
    aggregateV1 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.DELETED)
            .get("csm-user")!!
  }

  private fun query() =
      super.query(latestUserApi("/users"), false, UserListResource::class.java)
}
