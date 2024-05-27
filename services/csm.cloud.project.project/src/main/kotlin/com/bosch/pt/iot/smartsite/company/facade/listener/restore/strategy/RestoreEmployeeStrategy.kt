/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.EMPLOYEE
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreEmployeeStrategy(
    private val companyRepository: CompanyRepository,
    private val employeeRepository: EmployeeRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, employeeRepository),
    CompanyContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      EMPLOYEE.value == record.key().aggregateIdentifier.type &&
          record.value() is EmployeeEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val event = record.value() as EmployeeEventAvro?
    assertEventNotNull(event, record.key())

    if (event!!.getName() == DELETED) {
      deleteEmployee(event.getAggregate())
    } else if (event.getName() == CREATED || event.getName() == UPDATED) {
      val aggregate = event.getAggregate()
      val employee = findEmployee(aggregate.getAggregateIdentifier())

      if (employee == null) {
        createEmployee(aggregate)
      } else {
        updateEmployee(employee, aggregate)
      }
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun updateEmployee(employee: Employee, aggregate: EmployeeAggregateAvro) =
      update(
          employee,
          object : DetachedEntityUpdateCallback<Employee> {
            override fun update(entity: Employee) {
              setBasicAttributes(entity, aggregate)
              setRoles(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun createEmployee(aggregate: EmployeeAggregateAvro) {
    entityManager.persist(
        Employee().apply {
          setBasicAttributes(this, aggregate)
          setRoles(this, aggregate)
          setAuditAttributes(this, aggregate.auditingInformation)
        })
  }

  private fun setRoles(employee: Employee, aggregate: EmployeeAggregateAvro) {
    val roles = aggregate.getRoles().map { EmployeeRoleEnum.valueOf(it.name) }

    if (employee.roles == null) {
      employee.roles = roles.toMutableList()
    } else {
      employee.roles!!.clear()
      employee.roles!!.addAll(roles)
    }
  }

  private fun setBasicAttributes(employee: Employee, aggregate: EmployeeAggregateAvro) =
      employee
          .apply {
            identifier = UUID.fromString(aggregate.getAggregateIdentifier().getIdentifier())
            version = aggregate.getAggregateIdentifier().getVersion()
            user = findUserOrCreatePlaceholder(aggregate.getUser())
            company = findCompany(aggregate.getCompany())
          }
          .returnUnit()

  private fun deleteEmployee(aggregate: EmployeeAggregateAvro) =
      delete(
          employeeRepository.findOneWithDetailsByIdentifier(
              aggregate.getAggregateIdentifier().getIdentifier().toUUID()))

  private fun findEmployee(aggregateIdentifierAvro: AggregateIdentifierAvro): Employee? =
      employeeRepository.findOneWithDetailsByIdentifier(
          aggregateIdentifierAvro.getIdentifier().toUUID())

  private fun findCompany(aggregateIdentifierAvro: AggregateIdentifierAvro): Company? =
      companyRepository.findOneByIdentifier(aggregateIdentifierAvro.getIdentifier().toUUID())
}
