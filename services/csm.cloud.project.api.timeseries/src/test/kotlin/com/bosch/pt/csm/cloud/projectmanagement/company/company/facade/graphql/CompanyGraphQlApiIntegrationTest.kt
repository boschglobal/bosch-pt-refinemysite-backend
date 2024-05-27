/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class CompanyGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val company = "projects[0].participants[0].company"

  val query =
      """
      query {
        projects {
          participants {
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
          }
        }
      }
    """
          .trimIndent()

  lateinit var aggregateV0: CompanyAggregateAvro

  lateinit var aggregateV1: CompanyAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query company with all parameters set`() {
    submitEventsWithOptionalParameters()

    val aggregateV1 = get<CompanyAggregateAvro>("company")!!
    val sAddress = aggregateV1.streetAddress
    val pAddress = aggregateV1.postBoxAddress

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$company.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$company.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$company.name").isEqualTo(aggregateV1.name)
    response.get("$company.streetAddress.street").isEqualTo(sAddress.street)
    response.get("$company.streetAddress.houseNumber").isEqualTo(sAddress.houseNumber)
    response.get("$company.streetAddress.city").isEqualTo(sAddress.city)
    response.get("$company.streetAddress.zipCode").isEqualTo(sAddress.zipCode)
    response.get("$company.streetAddress.area").isEqualTo(sAddress.area)
    response.get("$company.streetAddress.country").isEqualTo(sAddress.country)
    response.get("$company.postBoxAddress.postBox").isEqualTo(pAddress.postBox)
    response.get("$company.postBoxAddress.city").isEqualTo(pAddress.city)
    response.get("$company.postBoxAddress.zipCode").isEqualTo(pAddress.zipCode)
    response.get("$company.postBoxAddress.area").isEqualTo(pAddress.area)
    response.get("$company.postBoxAddress.country").isEqualTo(pAddress.country)
    response.get("$company.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query company without optional parameters`() {
    submitEventsWithoutOptionalParameters()

    // Execute query and validate all fields
    val response = graphQlTester.document(query).execute()
    response.get("$company.id").isEqualTo(aggregateV1.getIdentifier().toString())
    response.get("$company.version").isEqualTo(aggregateV1.getVersion().toString())
    response.get("$company.name").isEqualTo(aggregateV1.name)
    response.get("$company.eventDate").isEqualTo(aggregateV1.eventDate())

    // Check optional attributes
    response.isNull("$company.streetAddress")
    response.isNull("$company.postBoxAddress")
  }

  @Test
  fun `query deleted company`() {
    submitEventsWithOptionalParameters()
    eventStreamGenerator.submitCompany("company", eventType = CompanyEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(company)
  }

  private fun submitEventsWithOptionalParameters() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("company")!!
    aggregateV1 =
        eventStreamGenerator
            .submitCompany("company", eventType = CompanyEventEnumAvro.UPDATED) {
              it.name = "Updated"
            }
            .get("company")!!
  }

  private fun submitEventsWithoutOptionalParameters() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("company")!!
    aggregateV1 =
        eventStreamGenerator
            .submitCompany("company", eventType = CompanyEventEnumAvro.UPDATED) {
              it.streetAddress = null
              it.postBoxAddress = null
            }
            .get("company")!!
  }
}
