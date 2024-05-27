/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asRole
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNotNull
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ParticipantGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val participant = "projects[0].participants[0]"

  val query =
      """
      query {
        projects {
          participants {
            id
            version
            company {
              id
              version
              name
              streetAddress {
                street
                houseNumber
                city
                zipCode
                area
                country
              }
              postBoxAddress {
                postBox
                city
                zipCode
                area
                country
              }
              eventDate
            }
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
            role
            status
            eventDate
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: ParticipantAggregateG3Avro

  lateinit var aggregateV1: ParticipantAggregateG3Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query participant with all parameters set`() {
    submitEventsWithOptionalParameters()

    val company = get<CompanyAggregateAvro>("company")!!
    val sAddress = company.streetAddress
    val pAddress = company.postBoxAddress

    val user = get<UserAggregateAvro>("csm-user")!!
    val phone = user.phoneNumbers.first()

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$participant.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$participant.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$participant.company.id").isEqualTo(company.getIdentifier().toString())
    response.get("$participant.company.version").isEqualTo(company.getVersion().toString())
    response.get("$participant.company.name").isEqualTo(company.name)
    response.get("$participant.company.streetAddress.street").isEqualTo(sAddress.street)
    response.get("$participant.company.streetAddress.houseNumber").isEqualTo(sAddress.houseNumber)
    response.get("$participant.company.streetAddress.city").isEqualTo(sAddress.city)
    response.get("$participant.company.streetAddress.zipCode").isEqualTo(sAddress.zipCode)
    response.get("$participant.company.streetAddress.area").isEqualTo(sAddress.area)
    response.get("$participant.company.streetAddress.country").isEqualTo(sAddress.country)
    response.get("$participant.company.postBoxAddress.postBox").isEqualTo(pAddress.postBox)
    response.get("$participant.company.postBoxAddress.city").isEqualTo(pAddress.city)
    response.get("$participant.company.postBoxAddress.zipCode").isEqualTo(pAddress.zipCode)
    response.get("$participant.company.postBoxAddress.area").isEqualTo(pAddress.area)
    response.get("$participant.company.postBoxAddress.country").isEqualTo(pAddress.country)
    response.get("$participant.company.eventDate").isEqualTo(company.eventDate())
    response.get("$participant.user.id").isEqualTo(user.getIdentifier().toString())
    response.get("$participant.user.version").isEqualTo(user.getVersion().toString())
    response.get("$participant.user.firstName").isEqualTo(user.firstName)
    response.get("$participant.user.lastName").isEqualTo(user.lastName)
    response.get("$participant.user.email").isEqualTo(user.email)
    response.get("$participant.user.position").isEqualTo(user.position)
    response.get("$participant.user.locale").isEqualTo(user.locale)
    response.get("$participant.user.country").isEqualTo(user.country.name)
    response.get("$participant.user.phoneNumbers[0].countryCode").isEqualTo(phone.countryCode)
    response
        .get("$participant.user.phoneNumbers[0].phoneNumberType")
        .isEqualTo(phone.phoneNumberType.name)
    response.get("$participant.user.phoneNumbers[0].callNumber").isEqualTo(phone.callNumber)
    response.get("$participant.user.eventDate").isEqualTo(user.eventDate())
    response.get("$participant.role").isEqualTo(aggregateV1.role.asRole().shortKey)
    response.get("$participant.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$participant.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query participant without optional parameters`() {
    submitEventsWithoutOptionalParameters()

    val company = get<CompanyAggregateAvro>("company")!!
    val user = get<UserAggregateAvro>("csm-user")!!

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$participant.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$participant.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$participant.company.id").isEqualTo(company.getIdentifier().toString())
    response.get("$participant.company.version").isEqualTo(company.getVersion().toString())
    response.get("$participant.company.name").isEqualTo(company.name)
    response.get("$participant.company.eventDate").isEqualTo(company.eventDate())
    response.get("$participant.user.id").isEqualTo(user.getIdentifier().toString())
    response.get("$participant.user.version").isEqualTo(user.getVersion().toString())
    response.get("$participant.user.firstName").isEqualTo(user.firstName)
    response.get("$participant.user.lastName").isEqualTo(user.lastName)
    response.get("$participant.user.email").isEqualTo(user.email)
    response.get("$participant.user.eventDate").isEqualTo(user.eventDate())
    response.get("$participant.role").isEqualTo(aggregateV1.role.asRole().shortKey)
    response.get("$participant.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$participant.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$participant.company.streetAddress")
    response.isNull("$participant.company.postBoxAddress")
    response.isNull("$participant.user.position")
    response.isNull("$participant.user.locale")
    response.isNull("$participant.user.country")
    response.get("$participant.user.phoneNumbers").isEqualTo(emptyList<Any>())
  }

  @Test
  fun `query participant with deleted user`() {
    submitEventsWithDeletedUser()

    val company = get<CompanyAggregateAvro>("company")!!

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$participant.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$participant.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$participant.company.id").isEqualTo(company.getIdentifier().toString())
    response.get("$participant.company.version").isEqualTo(company.getVersion().toString())
    response.get("$participant.company.name").isEqualTo(company.name)
    response.get("$participant.company.eventDate").isEqualTo(company.eventDate())
    response.isNotNull("$participant.user.id")
    response.get("$participant.user.version").isEqualTo("0")
    response.get("$participant.user.firstName").isEqualTo("Deleted")
    response.get("$participant.user.lastName").isEqualTo("User")
    response.get("$participant.user.email").isEqualTo("donotreply@bosch-refinemysite.com")
    response.get("$participant.user.eventDate").isEqualTo(LocalDateTime.MIN.toString())
    response.get("$participant.role").isEqualTo(aggregateV1.role.asRole().shortKey)
    response.get("$participant.status").isEqualTo(aggregateV1.status.asStatus().shortKey)
    response.get("$participant.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$participant.company.streetAddress")
    response.isNull("$participant.company.postBoxAddress")
    response.isNull("$participant.user.position")
    response.isNull("$participant.user.locale")
    response.isNull("$participant.user.country")
    response.get("$participant.user.phoneNumbers").isEqualTo(emptyList<Any>())
  }

  private fun submitEventsWithOptionalParameters() {
    eventStreamGenerator.submitProject().submitUser(
        "csm-user", eventType = UserEventEnumAvro.UPDATED) {
          it.position = "Position 1"
        }

    aggregateV0 = eventStreamGenerator.submitCsmParticipant().get("csm-participant")!!
    aggregateV1 =
        eventStreamGenerator
            .submitParticipantG3("csm-participant", eventType = ParticipantEventEnumAvro.UPDATED) {
              it.role = ParticipantRoleEnumAvro.CR
            }
            .get("csm-participant")!!
  }

  private fun submitEventsWithoutOptionalParameters() {
    eventStreamGenerator
        .submitCompany(eventType = CompanyEventEnumAvro.UPDATED) {
          it.postBoxAddress = null
          it.streetAddress = null
        }
        .submitUser("csm-user", eventType = UserEventEnumAvro.UPDATED) {
          it.position = null
          it.locale = null
          it.country = null
          it.phoneNumbers = emptyList()
        }
        .submitProject()

    aggregateV0 = eventStreamGenerator.submitCsmParticipant().get("csm-participant")!!
    aggregateV1 =
        eventStreamGenerator
            .submitParticipantG3("csm-participant", eventType = ParticipantEventEnumAvro.UPDATED) {
              it.role = ParticipantRoleEnumAvro.CR
            }
            .get("csm-participant")!!
  }

  private fun submitEventsWithDeletedUser() {
    eventStreamGenerator
        .submitCompany(eventType = CompanyEventEnumAvro.UPDATED) {
          it.postBoxAddress = null
          it.streetAddress = null
        }
        .submitUser("csm-user2", eventType = UserEventEnumAvro.CREATED) {
          it.position = null
          it.locale = null
          it.country = null
          it.phoneNumbers = emptyList()
        }
        .submitProject()
        .submitParticipantG3("csm-participant2") { it.user = getByReference("csm-user2") }
    aggregateV0 = eventStreamGenerator.submitCsmParticipant().get("csm-participant2")!!
    eventStreamGenerator.submitUser("csm-user2", eventType = UserEventEnumAvro.DELETED)

    aggregateV1 = eventStreamGenerator.get("csm-participant2")!!
  }
}
