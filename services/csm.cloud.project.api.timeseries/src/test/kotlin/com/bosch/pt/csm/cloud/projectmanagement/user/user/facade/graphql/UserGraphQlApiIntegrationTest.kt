/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql

import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNotNull
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.projectmanagement.user.user.extension.asNumberType
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class UserGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val user = "projects[0].participants[0].user"

  val query =
      """
      query {
        projects {
          participants {
            user {
              id
              version
              firstName
              lastName
              email
              position
              locale
              country
              phoneNumbers {
                countryCode
                phoneNumberType
                callNumber
              }
              eventDate
            }
          }
        }
      }
    """
          .trimIndent()

  lateinit var aggregateV0: UserAggregateAvro

  lateinit var aggregateV1: UserAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query users with all parameters set`() {
    submitEventsWithOptionalParameters()

    val phone = aggregateV1.phoneNumbers.first()

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$user.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$user.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$user.firstName").isEqualTo(aggregateV1.firstName)
    response.get("$user.lastName").isEqualTo(aggregateV1.lastName)
    response.get("$user.email").isEqualTo(aggregateV1.email)
    response.get("$user.position").isEqualTo(aggregateV1.position)
    response.get("$user.locale").isEqualTo(aggregateV1.locale)
    response.get("$user.country").isEqualTo(aggregateV1.country.name)
    response.get("$user.phoneNumbers[0].countryCode").isEqualTo(phone.countryCode)
    response
        .get("$user.phoneNumbers[0].phoneNumberType")
        .isEqualTo(phone.phoneNumberType.asNumberType().shortKey)
    response.get("$user.phoneNumbers[0].callNumber").isEqualTo(phone.callNumber)
    response.get("$user.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query user without optional parameters`() {
    submitEventsWithoutOptionalParameters()

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$user.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$user.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$user.firstName").isEqualTo(aggregateV1.firstName)
    response.get("$user.lastName").isEqualTo(aggregateV1.lastName)
    response.get("$user.email").isEqualTo(aggregateV1.email)
    response.get("$user.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$user.position")
    response.isNull("$user.locale")
    response.isNull("$user.country")
    response.get("$user.phoneNumbers").isEqualTo(emptyList<Any>())
  }

  @Test
  fun `query deleted users via deleted event`() {
    submitEventsWithOptionalParameters()
    eventStreamGenerator.submitUser("csm-user", eventType = UserEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNotNull("$user.id")
    response.get("$user.version").isEqualTo("0")
    response.get("$user.firstName").isEqualTo("Deleted")
    response.get("$user.lastName").isEqualTo("User")
    response.get("$user.email").isEqualTo("donotreply@bosch-refinemysite.com")
    response.isNull("$user.position")
    response.isNull("$user.locale")
    response.isNull("$user.country")
    response.path("$user.phoneNumbers").entityList(String::class.java).hasSize(0)
    response.get("$user.eventDate").isEqualTo(LocalDateTime.MIN.toString())
  }

  @Test
  fun `query deleted users via tombstone message`() {
    submitEventsWithOptionalParameters()
    val aggregate = get<UserAggregateAvro>("csm-user")!!
    eventStreamGenerator.submitUserTombstones(
        "csm-user",
        AggregateEventMessageKey(
            aggregate.aggregateIdentifier.buildAggregateIdentifier(), aggregate.getIdentifier()))

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNotNull("$user.id")
    response.get("$user.version").isEqualTo("0")
    response.get("$user.firstName").isEqualTo("Deleted")
    response.get("$user.lastName").isEqualTo("User")
    response.get("$user.email").isEqualTo("donotreply@bosch-refinemysite.com")
    response.isNull("$user.position")
    response.isNull("$user.locale")
    response.isNull("$user.country")
    response.path("$user.phoneNumbers").entityList(String::class.java).hasSize(0)
    response.get("$user.eventDate").isEqualTo(LocalDateTime.MIN.toString())
  }

  private fun submitEventsWithOptionalParameters() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
              it.position = "Position 1"
            }
            .get("csm-user")!!
    aggregateV1 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
              it.position = "Position 2"
            }
            .get("csm-user")!!
  }

  private fun submitEventsWithoutOptionalParameters() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
              it.position = null
              it.locale = null
              it.country = null
              it.phoneNumbers = emptyList()
            }
            .get("csm-user")!!

    aggregateV1 =
        eventStreamGenerator
            .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
              it.lastName = "Changed"
            }
            .get("csm-user")!!
  }
}
