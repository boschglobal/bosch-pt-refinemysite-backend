/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.boundary

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Employee
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.EmployeeRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service

@Service
class EmployeeService(private val employeeRepository: EmployeeRepository) {

  @Trace fun save(employee: Employee): Employee = employeeRepository.save(employee)

  @Trace
  fun findOneCachedByIdentifier(employeeAggregateIdentifier: AggregateIdentifier): Employee =
      employeeRepository.findOneCachedByIdentifier(employeeAggregateIdentifier)
}
