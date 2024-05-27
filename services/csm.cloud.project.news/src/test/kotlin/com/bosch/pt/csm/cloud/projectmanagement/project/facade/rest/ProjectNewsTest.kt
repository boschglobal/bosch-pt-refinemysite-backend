/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.deleteByProject
import com.bosch.pt.csm.cloud.projectmanagement.common.getList
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ProjectNewsTest : AbstractNewsTest() {

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun `is deleted after the user deleted it`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    controller.deleteByProject(projectAggregateIdentifier, employee)

    val taskIdentifiers =
        objectRelationRepository
            .findAllByLeftTypeAndRight(
                AggregateType.TASK, ObjectIdentifier(projectAggregateIdentifier))
            .map { it.left }
    assertThat(controller.getList(employee, taskIdentifiers)).isEmpty()
  }
}
