/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.model.Company
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class RestoreCompanyStrategyTest : AbstractRestoreIntegrationTestV2() {

  private val companyAggregate by lazy { get<CompanyAggregateAvro>("company")!! }
  private val company by lazy { repositories.findCompany(getIdentifier("company"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().submitCompanyWithBothAddresses()
  }

  @Test
  open fun `validate that company created event was processed successfully`() {
    val postBoxAggregateAvro = companyAggregate.getPostBoxAddress()
    val streetAggregateAvro = companyAggregate.getStreetAddress()

    validateBasicAttributes(company, companyAggregate)
    validatePostBoxAddress(company, postBoxAggregateAvro)
    validateStreetAddress(company, streetAggregateAvro)
    validateAuditableAndVersionedEntityAttributes(company, companyAggregate)
  }

  @Test
  open fun `validate that company updated event was process successfully`() {
    eventStreamGenerator.submitCompany(eventType = UPDATED) { it.name = "Updated name" }

    val postBoxAggregateAvro = companyAggregate.getPostBoxAddress()
    val streetAggregateAvro = companyAggregate.getStreetAddress()

    validateBasicAttributes(company, companyAggregate)
    validatePostBoxAddress(company, postBoxAggregateAvro)
    validateStreetAddress(company, streetAggregateAvro)
    validateAuditableAndVersionedEntityAttributes(company, companyAggregate)
  }

  @Test
  open fun `validate company deleted event marks a company as deleted`() {
    assertThat(repositories.companyRepository.findAll()).hasSize(1)

    eventStreamGenerator.submitCompany(eventType = DELETED)

    assertThat(company.deleted).isTrue
    validateBasicAttributes(company, companyAggregate)
    validateAuditableAndVersionedEntityAttributes(company, companyAggregate)

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)

    val company = repositories.findCompany(getIdentifier("company"))!!

    assertThat(company.deleted).isTrue
    validateBasicAttributes(company, companyAggregate)
    validateAuditableAndVersionedEntityAttributes(company, companyAggregate)
  }

  private fun validateBasicAttributes(company: Company, companyAggregate: CompanyAggregateAvro) {
    assertThat(company.name).isEqualTo(companyAggregate.getName())
  }

  private fun validatePostBoxAddress(company: Company, postBoxAggregateAvro: PostBoxAddressAvro) {
    val postBoxAddress = company.postBoxAddress!!

    assertThat(postBoxAddress.area).isEqualTo(postBoxAggregateAvro.getArea())
    assertThat(postBoxAddress.city).isEqualTo(postBoxAggregateAvro.getCity())
    assertThat(postBoxAddress.country).isEqualTo(postBoxAggregateAvro.getCountry())
    assertThat(postBoxAddress.postBox).isEqualTo(postBoxAggregateAvro.getPostBox())
    assertThat(postBoxAddress.zipCode).isEqualTo(postBoxAggregateAvro.getZipCode())
  }

  private fun validateStreetAddress(company: Company, streetAggregateAvro: StreetAddressAvro) {
    val streetAddress = company.streetAddress!!

    assertThat(streetAddress.area).isEqualTo(streetAggregateAvro.getArea())
    assertThat(streetAddress.city).isEqualTo(streetAggregateAvro.getCity())
    assertThat(streetAddress.country).isEqualTo(streetAggregateAvro.getCountry())
    assertThat(streetAddress.houseNumber).isEqualTo(streetAggregateAvro.getHouseNumber())
    assertThat(streetAddress.street).isEqualTo(streetAggregateAvro.getStreet())
    assertThat(streetAddress.zipCode).isEqualTo(streetAggregateAvro.getZipCode())
  }
}
