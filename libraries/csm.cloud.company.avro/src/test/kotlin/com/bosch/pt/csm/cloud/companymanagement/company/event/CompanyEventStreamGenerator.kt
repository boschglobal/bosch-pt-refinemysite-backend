/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
class CompanyEventStreamGenerator(
    private val timeLineGenerator: TimeLineGenerator,
    private val eventListener: CompanyEventListener,
    private val context: MutableMap<String, SpecificRecordBase>
) {

  fun submitCompany(
      name: String = "company",
      userName: String = "user",
      eventName: CompanyEventEnumAvro = CompanyEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((CompanyAggregateAvro) -> Unit)? = null
  ): CompanyEventStreamGenerator {
    val company = get<CompanyAggregateAvro?>(name)

    val defaultAggregateModifications: ((CompanyAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
    }

    context[name] =
        eventListener.submitCompany(
            existingCompany = company,
            eventName = eventName,
            companyAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }

  fun submitEmployee(
      name: String = "employee",
      companyName: String = "company",
      userName: String = "user",
      userReference: AggregateIdentifierAvro? = null,
      eventName: EmployeeEventEnumAvro = EmployeeEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((EmployeeAggregateAvro) -> Unit)? = null
  ): CompanyEventStreamGenerator {
    val company = get<CompanyAggregateAvro>(companyName)
    val user = get<SpecificRecordBase?>(userName)
    val employee = get<EmployeeAggregateAvro?>(name)

    val defaultAggregateModifications: ((EmployeeAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        user?.apply { setUser(get("aggregateIdentifier") as AggregateIdentifierAvro) }
        userReference?.apply { setUser(this) }
        setCompany(company.getAggregateIdentifier())
      }
    }

    context[name] =
        eventListener.submitEmployee(
            existingEmployee = employee,
            eventName = eventName,
            employeeAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }

  @Suppress("UNCHECKED_CAST") fun <T> get(name: String): T = context[name] as T

  private fun getAggregateIdentifier(aggregate: SpecificRecordBase) =
      aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro

  private fun setAuditingInformationAndIncreaseVersion(
      aggregate: SpecificRecordBase,
      eventName: String,
      userName: String,
      time: Instant
  ) {
    val auditingInformation = aggregate.get("auditingInformation") as AuditingInformationAvro
    val aggregateIdentifier = aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro
    val user = get<SpecificRecordBase?>(userName)
    when (eventName) {
      "CREATED" -> {
        user?.apply { auditingInformation.setCreatedBy(getAggregateIdentifier(this)) }
        auditingInformation.setCreatedDate(time.toEpochMilli())
      }
      else -> aggregateIdentifier.increase()
    }
    user?.apply { auditingInformation.setLastModifiedBy(getAggregateIdentifier(this)) }
    auditingInformation.setLastModifiedDate(time.toEpochMilli())
  }

  private fun AggregateIdentifierAvro.increase() = setVersion(getVersion() + 1)
}
