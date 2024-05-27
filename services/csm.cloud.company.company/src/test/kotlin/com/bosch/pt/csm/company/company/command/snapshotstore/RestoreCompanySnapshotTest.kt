/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.snapshotstore

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.common.util.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RestoreCompanySnapshotTest : AbstractRestoreIntegrationTest() {

  @Autowired private lateinit var cut: CompanySnapshotStore

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitCompanyWithBothAddresses()
  }

  @Test
  fun `company created event updates snapshot successfully`() {
    val companyAggregate = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!
    val postBoxAggregateAvro = companyAggregate.postBoxAddress
    val streetAggregateAvro = companyAggregate.streetAddress

    cut.findOrFail(companyAggregate.getIdentifier().asCompanyId()).apply {
      validateBasicAttributes(this, companyAggregate)
      validatePostBoxAddress(this, postBoxAggregateAvro)
      validateStreetAddress(this, streetAggregateAvro)
      validateAuditingInformation(this, companyAggregate)
    }
  }

  @Test
  fun `company updated event updates snapshot successfully`() {
    eventStreamGenerator.submitCompany(eventType = UPDATED) { it.name = "Updated name" }

    val companyAggregate = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!
    val postBoxAggregateAvro = companyAggregate.postBoxAddress
    val streetAggregateAvro = companyAggregate.streetAddress

    cut.findOrFail(companyAggregate.getIdentifier().asCompanyId()).apply {
      validateBasicAttributes(this, companyAggregate)
      validatePostBoxAddress(this, postBoxAggregateAvro)
      validateStreetAddress(this, streetAggregateAvro)
      validateAuditingInformation(this, companyAggregate)
    }
  }

  @Test
  fun `company deleted event deletes snapshot`() {
    eventStreamGenerator.submitCompany(eventType = DELETED).repeat(1)

    val companyAggregate = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    assertThatExceptionOfType(AggregateNotFoundException::class.java).isThrownBy {
      cut.findOrFail(companyAggregate.getIdentifier().asCompanyId())
    }
  }

  private fun validateBasicAttributes(
      company: CompanySnapshot,
      companyAggregate: CompanyAggregateAvro
  ) {
    assertThat(company.identifier.toUuid()).isEqualTo(companyAggregate.getIdentifier())
    assertThat(company.version).isEqualTo(companyAggregate.aggregateIdentifier.version)
    assertThat(company.name).isEqualTo(companyAggregate.name)
  }

  private fun validatePostBoxAddress(
      company: CompanySnapshot,
      postBoxAggregateAvro: PostBoxAddressAvro
  ) {
    assertThat(company.postBoxAddress).isNotNull
    company.postBoxAddress!!.apply {
      assertThat(area).isEqualTo(postBoxAggregateAvro.area)
      assertThat(city).isEqualTo(postBoxAggregateAvro.city)
      assertThat(country).isEqualTo(postBoxAggregateAvro.country)
      assertThat(postBox).isEqualTo(postBoxAggregateAvro.postBox)
      assertThat(zipCode).isEqualTo(postBoxAggregateAvro.zipCode)
    }
  }

  private fun validateStreetAddress(
      company: CompanySnapshot,
      streetAggregateAvro: StreetAddressAvro
  ) {
    assertThat(company.streetAddress).isNotNull
    company.streetAddress!!.apply {
      assertThat(area).isEqualTo(streetAggregateAvro.area)
      assertThat(city).isEqualTo(streetAggregateAvro.city)
      assertThat(country).isEqualTo(streetAggregateAvro.country)
      assertThat(houseNumber).isEqualTo(streetAggregateAvro.houseNumber)
      assertThat(street).isEqualTo(streetAggregateAvro.street)
      assertThat(zipCode).isEqualTo(streetAggregateAvro.zipCode)
    }
  }
}
