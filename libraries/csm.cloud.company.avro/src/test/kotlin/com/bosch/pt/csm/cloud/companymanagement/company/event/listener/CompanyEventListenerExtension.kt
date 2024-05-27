/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.event.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.randomCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.randomEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

fun CompanyEventListener.submitCompany(
    existingCompany: CompanyAggregateAvro? = null,
    messageKey: AggregateEventMessageKey? = null,
    eventName: CompanyEventEnumAvro = CompanyEventEnumAvro.CREATED,
    vararg companyAggregateOperations: ((CompanyAggregateAvro) -> Unit)?
): CompanyAggregateAvro {
  val company = existingCompany.buildEventAvro(eventName, *companyAggregateOperations)

  val aggregateIdentifier = company.getAggregate().getAggregateIdentifier()
  val key =
      messageKey
          ?: AggregateEventMessageKey(
              aggregateIdentifier.buildAggregateIdentifier(),
              aggregateIdentifier.getIdentifier().toUUID())

  return submitEvent(key, company, ::listenToCompanyEvents).getAggregate()
}

fun CompanyEventListener.submitEmployee(
    existingEmployee: EmployeeAggregateAvro? = null,
    messageKey: AggregateEventMessageKey? = null,
    eventName: EmployeeEventEnumAvro = EmployeeEventEnumAvro.CREATED,
    vararg employeeAggregateOperations: ((EmployeeAggregateAvro) -> Unit)?
): EmployeeAggregateAvro {
  val employee = existingEmployee.buildEventAvro(eventName, *employeeAggregateOperations)

  val aggregate = employee.getAggregate()
  val key =
      messageKey
          ?: AggregateEventMessageKey(
              aggregate.getAggregateIdentifier().buildAggregateIdentifier(),
              aggregate.getCompany().getIdentifier().toUUID())

  return submitEvent(key, employee, ::listenToCompanyEvents).getAggregate()
}

@Suppress("unused")
fun <V : SpecificRecordBase?> CompanyEventListener.submitEvent(
    key: EventMessageKey,
    value: V,
    listener: (ConsumerRecord<EventMessageKey, SpecificRecordBase?>, Acknowledgment) -> Unit
): V {
  mockk<Acknowledgment>(relaxed = true).apply {
    listener(ConsumerRecord("", 0, 0, key, value), this)
    verify { acknowledge() }
  }
  return value
}

private fun CompanyAggregateAvro?.buildEventAvro(
    event: CompanyEventEnumAvro,
    vararg blocks: ((CompanyAggregateAvro) -> Unit)?
) =
    (this?.let { CompanyEventAvro(event, this) } ?: randomCompany(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun EmployeeAggregateAvro?.buildEventAvro(
    event: EmployeeEventEnumAvro,
    vararg blocks: ((EmployeeAggregateAvro) -> Unit)?
) =
    (this?.let { EmployeeEventAvro(event, this) } ?: randomEmployee(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }
