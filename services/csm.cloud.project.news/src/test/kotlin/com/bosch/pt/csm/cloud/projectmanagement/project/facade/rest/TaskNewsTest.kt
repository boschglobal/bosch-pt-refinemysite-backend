/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.common.getList
import com.bosch.pt.csm.cloud.projectmanagement.common.matchesNewsInAnyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TaskNewsTest : AbstractNewsTest() {

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun `is deleted after the user deleted it`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    controller.delete(taskAggregateIdentifier, employee)
    assertThat(controller.getDetails(employee, taskAggregateIdentifier)).isEmpty()
  }

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun details(@Suppress("UNUSED_PARAMETER") role: String?, employee: () -> EmployeeAggregateAvro) {
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(taskAggregateIdentifier, taskAggregateIdentifier, taskAggregateIdentifier))
  }

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun `is created after a change of task`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    assertThat(controller.getList(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(taskAggregateIdentifier, taskAggregateIdentifier, taskAggregateIdentifier))
  }
}
