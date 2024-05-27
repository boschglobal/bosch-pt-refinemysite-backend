/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.company.model.Employee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreEmployeeStrategyTest : AbstractRestoreIntegrationTestV2() {

  private val employeeAggregate by lazy { get<EmployeeAggregateAvro>("employee")!! }
  private val employee by lazy {
    repositories.employeeRepository.findOneWithDetailsByIdentifier(getIdentifier("employee"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUser("daniel")
        .submitCraft()
        .submitCompanyWithBothAddresses()
        .submitEmployee { it.roles = listOf(CSM) }
  }

  @Test
  open fun `validate that employee created event was processed successfully`() {
    validateEmployeeAttributes(employee, employeeAggregate)
  }

  @Test
  open fun `validate that employee updated event was processed successfully`() {
    eventStreamGenerator.submitEmployee(eventType = UPDATED) { it.roles = listOf(FM) }

    validateEmployeeAttributes(employee, employeeAggregate)
  }

  @Test
  open fun `validate that employee deleted event was processed successfully`() {
    val employeeIdentifier = getIdentifier("employee")

    assertThat(repositories.findEmployee(employeeIdentifier)).isNotNull

    eventStreamGenerator.submitEmployee(eventType = DELETED)

    assertThat(repositories.findEmployee(employeeIdentifier)).isNull()

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)

    assertThat(repositories.findEmployee(employeeIdentifier)).isNull()
  }

  // TODO: should an employee not be created if it is not deleted? currently it does not matter
  // whether employee is deleted or not, it can be created (results in 2 employees with different
  // identifiers but same attributes, if it is not deleted)
  @Test
  open fun `validate employee can be created after delete`() {
    val employeeIdentifier = getIdentifier("employee")

    eventStreamGenerator.submitEmployee(eventType = DELETED).submitEmployee("newEmployee") {
      it.roles = listOf(CSM)
    }

    assertThat(repositories.employeeRepository.findOneWithDetailsByIdentifier(employeeIdentifier))
        .isNull()

    val newEmployee =
        repositories.employeeRepository.findOneWithDetailsByIdentifier(getIdentifier("newEmployee"))

    assertThat(newEmployee).isNotNull
    validateEmployeeAttributes(newEmployee!!, get("newEmployee")!!)
  }

  private fun validateEmployeeAttributes(
      employee: Employee,
      employeeAggregate: EmployeeAggregateAvro
  ) {
    validateAuditableAndVersionedEntityAttributes(employee, employeeAggregate)
    assertThat(employee.user!!.identifier)
        .isEqualTo(employeeAggregate.getUser().getIdentifier().toUUID())
    assertThat(employee.company!!.identifier)
        .isEqualTo(employeeAggregate.getCompany().getIdentifier().toUUID())
    assertThat(employee.roles!!.map { it.name })
        .isEqualTo(employeeAggregate.getRoles().map { it.name })
  }
}
