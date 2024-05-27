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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asRole
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskAssigneeGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val participant = "projects[0].tasks[0].assignee"

  val query =
      """
      query {
        projects {
          tasks {
            assignee {
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
      }
      """
          .trimIndent()

  lateinit var aggregate: ParticipantAggregateG3Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query assignee with all parameters set`() {
    submitEventsWithOptionalParameters()

    val company = get<CompanyAggregateAvro>("company")!!
    val sAddress = company.streetAddress
    val pAddress = company.postBoxAddress

    val user = get<UserAggregateAvro>("csm-user")!!
    val phone = user.phoneNumbers.first()

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$participant.id").isEqualTo(aggregate.getIdentifier().toString())
    response.get("$participant.version").isEqualTo(aggregate.getVersion().toString())
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
    response.get("$participant.role").isEqualTo(aggregate.role.asRole().shortKey)
    response.get("$participant.status").isEqualTo(aggregate.status.asStatus().shortKey)
    response.get("$participant.eventDate").isEqualTo(aggregate.eventDate())
  }

  @Test
  fun `query assignee without optional parameters`() {
    submitEventsWithoutOptionalParameters()

    val company = get<CompanyAggregateAvro>("company")!!
    val user = get<UserAggregateAvro>("csm-user")!!

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$participant.id").isEqualTo(aggregate.getIdentifier().toString())
    response.get("$participant.version").isEqualTo(aggregate.getVersion().toString())
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
    response.get("$participant.role").isEqualTo(aggregate.role.asRole().shortKey)
    response.get("$participant.status").isEqualTo(aggregate.status.asStatus().shortKey)
    response.get("$participant.eventDate").isEqualTo(aggregate.eventDate())

    // Check optional attributes
    response.isNull("$participant.company.streetAddress")
    response.isNull("$participant.company.postBoxAddress")
    response.isNull("$participant.user.position")
    response.isNull("$participant.user.locale")
    response.isNull("$participant.user.country")
    response.get("$participant.user.phoneNumbers").isEqualTo(emptyList<Any>())
  }

  private fun submitEventsWithOptionalParameters() {
    eventStreamGenerator.submitProject().submitProjectCraftG2().submitUser(
        "csm-user", eventType = UserEventEnumAvro.UPDATED) {
          it.position = "Position 1"
        }

    aggregate = eventStreamGenerator.submitCsmParticipant().get("csm-participant")!!

    eventStreamGenerator.submitTask { it.assignee = getByReference("csm-participant") }
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
        .submitProjectCraftG2()

    aggregate = eventStreamGenerator.submitCsmParticipant().get("csm-participant")!!

    eventStreamGenerator.submitTask { it.assignee = getByReference("csm-participant") }
  }
}
