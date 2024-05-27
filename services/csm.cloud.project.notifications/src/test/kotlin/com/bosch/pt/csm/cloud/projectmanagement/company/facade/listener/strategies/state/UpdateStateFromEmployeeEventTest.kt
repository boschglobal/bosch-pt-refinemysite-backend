/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.EMPLOYEE
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.test.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
@DisplayName("State must be updated upon an employee event")
@SmartSiteSpringBootTest
class UpdateStateFromEmployeeEventTest : BaseNotificationTest() {

  @Test
  fun `and company deleted from database`() {
    var employee =
        repositories.employeeRepository.findById(
            getByReference(CSM_EMPLOYEE).toAggregateIdentifier())
    assertThat(employee.isPresent).isTrue
    assertThat(employee.get().deleted).isFalse

    eventStreamGenerator.submitEmployee(asReference = CSM_EMPLOYEE, eventType = DELETED)

    employee =
        repositories.employeeRepository.findById(
            getByReference(CSM_EMPLOYEE).toAggregateIdentifier())
    assertThat(employee.isPresent).isTrue
    assertThat(employee.get().deleted).isTrue
  }

  @Test
  fun `where the user is deleted and re-assigned to the same company`() {
    // Re-assign user to the same company
    eventStreamGenerator.submitEmployee(asReference = CSM_EMPLOYEE, eventType = DELETED)
    val originalAggregateIdentifier = getByReference(CSM_EMPLOYEE).toAggregateIdentifier()
    eventStreamGenerator.submitEmployee(asReference = CSM_EMPLOYEE) {
      it.aggregateIdentifierBuilder =
          AggregateIdentifierAvro.newBuilder()
              .setIdentifier(randomUUID().toString())
              .setVersion(0L)
              .setType(EMPLOYEE.value)
      it.user = getByReference(CSM_USER)
    }

    val newAggregateIdentifier = getByReference(CSM_EMPLOYEE).toAggregateIdentifier()

    // Check that the old employee is marked as deleted
    val originalEmployee = repositories.employeeRepository.findById(originalAggregateIdentifier)
    assertThat(originalEmployee.isPresent).isTrue
    assertThat(originalEmployee.get().deleted).isTrue

    val newEmployee = repositories.employeeRepository.findById(newAggregateIdentifier)
    assertThat(newEmployee.isPresent).isTrue
    assertThat(newEmployee.get().deleted).isFalse
  }

  @Test
  fun `where the user is re-assigned to another company`() {
    eventStreamGenerator
        .submitEmployee(asReference = CSM_EMPLOYEE, eventType = DELETED)
        .setLastIdentifierForType(CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY_2))
        .submitEmployee(asReference = "csm-employee2") { it.user = getByReference(CSM_USER) }

    // Check that the old employee is marked as deleted
    val employee1 =
        repositories.employeeRepository.findById(
            getByReference(CSM_EMPLOYEE).toAggregateIdentifier())
    assertThat(employee1.isPresent).isTrue
    assertThat(employee1.get().deleted).isTrue

    // Check that the new employee is not marked as deleted
    val employee2 =
        repositories.employeeRepository.findById(
            getByReference("csm-employee2").toAggregateIdentifier())
    assertThat(employee2.isPresent).isTrue
    assertThat(employee2.get().deleted).isFalse
  }
}
