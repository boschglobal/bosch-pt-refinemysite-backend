/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.CompanyReferencedAggregateTypesEnum.USER
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.FM
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitEmployee(
    asReference: String = "employee",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[COMPANY.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: EmployeeEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((EmployeeAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingEmployee = get<EmployeeAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((EmployeeAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((EmployeeAggregateAvro.Builder) -> Unit) = {
    it.company = it.company ?: getContext().lastIdentifierPerType[COMPANY.value]
    it.user = it.user ?: getContext().lastIdentifierPerType[USER.value]
  }

  val employeeEvent =
      existingEmployee.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          employeeEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send("company", asReference, messageKey, employeeEvent, time.toEpochMilli())
          as EmployeeEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun EmployeeAggregateAvro?.buildEventAvro(
    eventType: EmployeeEventEnumAvro,
    vararg blocks: ((EmployeeAggregateAvro.Builder) -> Unit)?
): EmployeeEventAvro =
    (this?.let { EmployeeEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newEmployee(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newEmployee(event: EmployeeEventEnumAvro = CREATED): EmployeeEventAvro.Builder {
  val employee =
      EmployeeAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(CompanymanagementAggregateTypeEnum.EMPLOYEE.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setRoles(listOf(FM))

  return EmployeeEventAvro.newBuilder().setAggregateBuilder(employee).setName(event)
}
