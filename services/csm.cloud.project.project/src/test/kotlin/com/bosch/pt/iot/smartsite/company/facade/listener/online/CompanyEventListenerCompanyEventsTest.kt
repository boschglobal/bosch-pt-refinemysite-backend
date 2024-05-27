/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.online

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.model.Company
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@EnableAllKafkaListeners
open class CompanyEventListenerCompanyEventsTest : AbstractIntegrationTestV2() {

  private val companyAggregate by lazy { get<CompanyAggregateAvro>("company")!! }
  private val company by lazy {
    repositories.companyRepository.findOneByIdentifier(getIdentifier("company"))!!
  }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().submitUser("daniel")
    useOnlineListener()
  }

  @Test
  fun `validate company created event`() {
    eventStreamGenerator.submitCompanyWithBothAddresses()

    assertCompanyFields(company, companyAggregate)

    // test idempotency
    eventStreamGenerator.repeat(1)

    repositories.companyRepository.findOneByIdentifier(companyAggregate.getIdentifier())!!.apply {
      assertCompanyFields(this, companyAggregate)
    }
  }

  @Test
  fun `validate company deleted event company without participants using company`() {
    eventStreamGenerator.submitCompanyWithBothAddresses()
    val companyIdentifier = getIdentifier("company")

    assertThat(repositories.companyRepository.findOneByIdentifier(companyIdentifier)).isNotNull

    eventStreamGenerator.submitCompany(eventType = CompanyEventEnumAvro.DELETED)

    assertThat(repositories.companyRepository.findOneByIdentifier(companyIdentifier)).isNull()

    // test idempotency
    assertDoesNotThrow { eventStreamGenerator.repeat(1) }

    assertThat(repositories.companyRepository.findOneByIdentifier(companyIdentifier)).isNull()
  }

  @Test
  fun `validate company deleted event with participants using company`() {
    eventStreamGenerator
        .submitCompanyWithBothAddresses()
        .submitEmployee {
          it.user = getByReference("daniel")
          it.roles = listOf(EmployeeRoleEnumAvro.FM)
        }
        .setUserContext("daniel")

    setAuthentication(getIdentifier("daniel"))

    val originalCompany =
        repositories.companyRepository
            .findOneByIdentifier(companyAggregate.getIdentifier())!!.apply {
              assertThat(deleted).isFalse()
            }

    eventStreamGenerator
        .submitProject()
        .submitParticipantG3 {
          it.user = getByReference("daniel")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitEmployee(eventType = EmployeeEventEnumAvro.DELETED, auditUserReference = "daniel")
        .submitCompany(eventType = CompanyEventEnumAvro.DELETED, auditUserReference = "system")

    repositories.companyRepository.findOneByIdentifier(companyAggregate.getIdentifier())!!.apply {
      assertDeletedCompanyFields(this, originalCompany)
    }

    // test idempotency
    eventStreamGenerator.repeat(1)

    repositories.companyRepository.findOneByIdentifier(companyAggregate.getIdentifier())!!.apply {
      assertDeletedCompanyFields(this, originalCompany)
    }
  }

  private fun assertCompanyFields(company: Company, companyAggregate: CompanyAggregateAvro) {
    assertThat(company.name).isEqualTo(companyAggregate.getName())
    assertThat(company.identifier)
        .isEqualTo(companyAggregate.getAggregateIdentifier().getIdentifier().toUUID())
    assertThat(company.deleted).isFalse
    assertThat(company.streetAddress).isNotNull
    assertThat(company.streetAddress!!.street)
        .isEqualTo(companyAggregate.getStreetAddress().getStreet())
    assertThat(company.streetAddress!!.houseNumber)
        .isEqualTo(companyAggregate.getStreetAddress().getHouseNumber())
    assertThat(company.streetAddress!!.area)
        .isEqualTo(companyAggregate.getStreetAddress().getArea())
    assertThat(company.streetAddress!!.city)
        .isEqualTo(companyAggregate.getStreetAddress().getCity())
    assertThat(company.streetAddress!!.country)
        .isEqualTo(companyAggregate.getStreetAddress().getCountry())
    assertThat(company.streetAddress!!.zipCode)
        .isEqualTo(companyAggregate.getStreetAddress().getZipCode())

    assertThat(company.postBoxAddress).isNotNull
    assertThat(company.postBoxAddress!!.postBox)
        .isEqualTo(companyAggregate.getPostBoxAddress().getPostBox())
    assertThat(company.postBoxAddress!!.area)
        .isEqualTo(companyAggregate.getPostBoxAddress().getArea())
    assertThat(company.postBoxAddress!!.city)
        .isEqualTo(companyAggregate.getPostBoxAddress().getCity())
    assertThat(company.postBoxAddress!!.country)
        .isEqualTo(companyAggregate.getPostBoxAddress().getCountry())
    assertThat(company.postBoxAddress!!.zipCode)
        .isEqualTo(companyAggregate.getPostBoxAddress().getZipCode())
    assertThat(company.version).isEqualTo(companyAggregate.getAggregateIdentifier().getVersion())
  }

  private fun assertDeletedCompanyFields(company: Company, originalCompany: Company) {
    assertThat(company.deleted).isTrue
    assertThat(company.version).isEqualTo(1)
    assertThat(company.name).isEqualTo(originalCompany.name)
    assertThat(company.postBoxAddress!!.area).isEqualTo(originalCompany.postBoxAddress!!.area)
    assertThat(company.postBoxAddress!!.city).isEqualTo(originalCompany.postBoxAddress!!.city)
    assertThat(company.postBoxAddress!!.country).isEqualTo(originalCompany.postBoxAddress!!.country)
    assertThat(company.postBoxAddress!!.postBox).isEqualTo(originalCompany.postBoxAddress!!.postBox)
    assertThat(company.postBoxAddress!!.zipCode).isEqualTo(originalCompany.postBoxAddress!!.zipCode)
  }
}
