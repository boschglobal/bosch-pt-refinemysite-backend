/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getCompanyIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.company.boundary.EmployeeService
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Employee
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromEmployeeEvent(private val employeeService: EmployeeService) :
    AbstractStateStrategy<EmployeeEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) = record.value is EmployeeEventAvro

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: EmployeeEventAvro): Unit =
      event.aggregate.run {
        employeeService.save(
            Employee(
                identifier = buildAggregateIdentifier(),
                companyIdentifier = getCompanyIdentifier(),
                userIdentifier = getUserIdentifier(),
                deleted = event.name == EmployeeEventEnumAvro.DELETED))
      }
}
