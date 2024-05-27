/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.EMPLOYEE
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.company.shared.repository.CompanyRepository
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum.valueOf
import com.bosch.pt.csm.company.employee.shared.repository.EmployeeRepository
import com.bosch.pt.csm.company.eventstore.CompanyContextSnapshotStore
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class EmployeeSnapshotStore(
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository
) :
    AbstractSnapshotStoreJpa<EmployeeEventAvro, EmployeeSnapshot, Employee, EmployeeId>(),
    CompanyContextSnapshotStore {

  override fun findOrFail(identifier: EmployeeId): EmployeeSnapshot =
      employeeRepository.findOneWithDetailsByIdentifier(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              EMPLOYEE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  fun findOrFailByUserId(identifier: UserId): EmployeeSnapshot =
      employeeRepository.findOneByUserRef(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              EMPLOYEE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      EMPLOYEE.value == key.aggregateIdentifier.type && message is EmployeeEventAvro

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as EmployeeEventAvro).name === DELETED

  override fun updateInternal(event: EmployeeEventAvro, currentSnapshot: Employee?): Long =
      if (event.name == DELETED && currentSnapshot != null) {
        deleteEmployee(currentSnapshot)
      } else {
        when (currentSnapshot == null) {
          true -> createEmployee(event)
          false -> updateEmployee(currentSnapshot, event)
        }
      }

  override fun findInternal(identifier: UUID): Employee? =
      employeeRepository.findOneByIdentifier(EmployeeId(identifier))

  private fun createEmployee(event: EmployeeEventAvro) = updateEmployee(Employee(), event)

  private fun updateEmployee(employee: Employee, event: EmployeeEventAvro): Long {
    val aggregate = event.aggregate
    employee.apply {
      setBasicAttributes(this, aggregate)
      setRoles(this, aggregate)
      setAuditAttributes(this, aggregate.auditingInformation)
      return employeeRepository.saveAndFlush(this).version
    }
  }

  private fun deleteEmployee(employee: Employee) =
      employeeRepository.delete(employee).let { employee.version + 1 }

  private fun setRoles(employee: Employee, aggregate: EmployeeAggregateAvro) {
    val roles = aggregate.roles.map { valueOf(it.name) }.toMutableList()

    if (employee.roles == null) {
      employee.roles = roles
    } else {
      employee.roles!!.clear()
      employee.roles!!.addAll(roles)
    }
  }

  private fun setBasicAttributes(employee: Employee, aggregate: EmployeeAggregateAvro) {
    employee.apply {
      identifier = EmployeeId(aggregate.aggregateIdentifier.identifier.toUUID())
      userRef = aggregate.user.identifier.toUUID().asUserId()
      company = checkNotNull(findCompany(aggregate.company))
    }
  }

  private fun findCompany(aggregateIdentifierAvro: AggregateIdentifierAvro): Company? =
      companyRepository.findOneByIdentifier(
          aggregateIdentifierAvro.identifier.toUUID().asCompanyId())
}
