/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.common.util.getIdentifier
import com.bosch.pt.csm.common.util.toUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RestoreEmployeeSnapshotTest : AbstractRestoreIntegrationTest() {

  @Autowired lateinit var cut: EmployeeSnapshotStore

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUser("daniel").submitCompany()
  }

  @Test
  fun `employee created event updates snapshot successfully`() {
    eventStreamGenerator.submitEmployee("danielEmployee")

    val employeeAggregate = eventStreamGenerator.get<EmployeeAggregateAvro>("danielEmployee")!!

    cut.findOrFail(employeeAggregate.getIdentifier().asEmployeeId()).apply {
      validateBasicAttributes(this, employeeAggregate)
      validateAuditingInformation(this, employeeAggregate)
    }
  }

  @Test
  fun `employee updated event updates snapshot successfully`() {
    eventStreamGenerator.submitEmployee("danielEmployee").submitEmployee(
        "danielEmployee", eventType = UPDATED) { it.roles = listOf(EmployeeRoleEnumAvro.FM) }

    val employeeAggregate = eventStreamGenerator.get<EmployeeAggregateAvro>("danielEmployee")!!

    cut.findOrFail(employeeAggregate.getIdentifier().asEmployeeId()).apply {
      validateBasicAttributes(this, employeeAggregate)
      validateAuditingInformation(this, employeeAggregate)
    }
  }

  @Test
  fun `employee deleted event removes snapshot`() {
    eventStreamGenerator
        .submitEmployee("danielEmployee")
        .submitEmployee("danielEmployee", eventType = DELETED)

    val employeeIdentifier = eventStreamGenerator.getIdentifier("danielEmployee")

    assertThatExceptionOfType(AggregateNotFoundException::class.java).isThrownBy {
      cut.findOrFail(employeeIdentifier.asEmployeeId())
    }

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  private fun validateBasicAttributes(
      employee: EmployeeSnapshot,
      employeeAggregate: EmployeeAggregateAvro
  ) {
    assertThat(employee.identifier).isEqualTo(employeeAggregate.getIdentifier().asEmployeeId())
    assertThat(employee.version).isEqualTo(employeeAggregate.aggregateIdentifier.version)
    assertThat(employee.userRef).isEqualTo(employeeAggregate.user.identifier.toUUID().asUserId())
    assertThat(employee.companyRef)
        .isEqualTo(employeeAggregate.company.identifier.toUUID().asCompanyId())
    assertThat(employee.roles.map { it.name }).isEqualTo(employeeAggregate.roles.map { it.name })
  }
}
