/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.asCompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class CompanyRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: CompanyAggregateAvro

  lateinit var aggregateV1: CompanyAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query company`() {
    submitEvents()

    // Execute query
    val companyList = query()

    // Validate payload
    assertThat(companyList.companies).hasSize(1)
    val companyV1 = companyList.companies.first()
    assertThat(aggregateV1.streetAddress).isNotNull()
    assertThat(aggregateV1.postBoxAddress).isNotNull()
    val sAddress = aggregateV1.streetAddress
    val pAddress = aggregateV1.postBoxAddress

    assertThat(companyV1.id).isEqualTo(aggregateV1.getIdentifier().asCompanyId())
    assertThat(companyV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(companyV1.name).isEqualTo(aggregateV1.name)
    assertThat(companyV1.streetAddress?.street).isEqualTo(sAddress.street)
    assertThat(companyV1.streetAddress?.houseNumber).isEqualTo(sAddress.houseNumber)
    assertThat(companyV1.streetAddress?.city).isEqualTo(sAddress.city)
    assertThat(companyV1.streetAddress?.zipCode).isEqualTo(sAddress.zipCode)
    assertThat(companyV1.streetAddress?.area).isEqualTo(sAddress.area)
    assertThat(companyV1.streetAddress?.country).isEqualTo(sAddress.country)
    assertThat(companyV1.postBoxAddress?.postBox).isEqualTo(pAddress.postBox)
    assertThat(companyV1.postBoxAddress?.city).isEqualTo(pAddress.city)
    assertThat(companyV1.postBoxAddress?.zipCode).isEqualTo(pAddress.zipCode)
    assertThat(companyV1.postBoxAddress?.area).isEqualTo(pAddress.area)
    assertThat(companyV1.postBoxAddress?.country).isEqualTo(pAddress.country)
    assertThat(companyV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(companyV1.deleted).isFalse()
  }

  @Test
  fun `query deleted company`() {
    submitAsDeletedEvents()

    // Execute query
    val companyList = query()

    // Validate payload
    assertThat(companyList.companies).hasSize(1)
    val companyV1 = companyList.companies.first()
    assertThat(aggregateV1.streetAddress).isNotNull()
    assertThat(aggregateV1.postBoxAddress).isNotNull()
    val sAddress = aggregateV1.streetAddress
    val pAddress = aggregateV1.postBoxAddress

    assertThat(companyV1.id).isEqualTo(aggregateV1.getIdentifier().asCompanyId())
    assertThat(companyV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(companyV1.name).isEqualTo(aggregateV1.name)
    assertThat(companyV1.streetAddress?.street).isEqualTo(sAddress.street)
    assertThat(companyV1.streetAddress?.houseNumber).isEqualTo(sAddress.houseNumber)
    assertThat(companyV1.streetAddress?.city).isEqualTo(sAddress.city)
    assertThat(companyV1.streetAddress?.zipCode).isEqualTo(sAddress.zipCode)
    assertThat(companyV1.streetAddress?.area).isEqualTo(sAddress.area)
    assertThat(companyV1.streetAddress?.country).isEqualTo(sAddress.country)
    assertThat(companyV1.postBoxAddress?.postBox).isEqualTo(pAddress.postBox)
    assertThat(companyV1.postBoxAddress?.city).isEqualTo(pAddress.city)
    assertThat(companyV1.postBoxAddress?.zipCode).isEqualTo(pAddress.zipCode)
    assertThat(companyV1.postBoxAddress?.area).isEqualTo(pAddress.area)
    assertThat(companyV1.postBoxAddress?.country).isEqualTo(pAddress.country)
    assertThat(companyV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(companyV1.deleted).isTrue()
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("company")!!
    aggregateV1 =
        eventStreamGenerator
            .submitCompany("company", eventType = CompanyEventEnumAvro.UPDATED) {
              it.name = "Updated"
            }
            .get("company")!!
  }

  private fun submitAsDeletedEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 = eventStreamGenerator.get("company")!!
    aggregateV1 =
        eventStreamGenerator
            .submitCompany("company", eventType = CompanyEventEnumAvro.DELETED)
            .get("company")!!
  }

  private fun query() =
      super.query(latestCompanyApi("/companies"), false, CompanyListResource::class.java)
}
