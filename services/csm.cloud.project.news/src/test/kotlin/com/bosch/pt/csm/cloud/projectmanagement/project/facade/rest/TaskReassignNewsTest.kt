/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.common.matchesNewsInAnyOrder
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TaskReassignNewsTest : AbstractNewsTest() {

  @BeforeEach
  fun initialize() {
    eventStreamGenerator.submitParticipantG3(asReference = "other-company-fm-participant") {
      it.user = getByReference("other-company-fm-user")
      it.role = FM
    }
  }

  @Test
  fun `for the previous assignee is deleted`() {
    eventStreamGenerator
        .submitParticipantG3(
            asReference = "fm-participant", eventType = ParticipantEventEnumAvro.DEACTIVATED)
        .submitTask(eventType = ASSIGNED) { it.assignee = getByReference("csm-participant-1") }
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `for the new assignee is created`() {
    val reassignedNewsDate = Instant.now().plusSeconds(3)
    controller.delete(taskAggregateIdentifier, employeeCsm1)
    eventStreamGenerator.submitTask(eventType = ASSIGNED) {
      it.assignee = getByReference("other-company-fm-participant")
      it.auditingInformationBuilder.lastModifiedDate = reassignedNewsDate.toEpochMilli()
    }

    assertThat(
            controller.getDetails(
                (get("other-company-fm-employee") as EmployeeAggregateAvro?)!!,
                taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                reassignedNewsDate,
                reassignedNewsDate))
  }

  @MethodSource(employeesCsmCr)
  @ParameterizedTest(name = "for {0}")
  fun `still exist`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    val reassignedNewsDate = Instant.now().plusSeconds(3)
    eventStreamGenerator.submitTask(eventType = ASSIGNED) {
      it.assignee = getByReference("other-company-fm-participant")
      it.auditingInformationBuilder.lastModifiedDate = reassignedNewsDate.toEpochMilli()
    }
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                lastModifiedDate = reassignedNewsDate))
  }

  @MethodSource(employeesCsmCr)
  @ParameterizedTest(name = "for {0}")
  fun `is created`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    val reassignedNewsDate = Instant.now().plusSeconds(3)
    controller.delete(taskAggregateIdentifier, employee)
    eventStreamGenerator.submitTask(eventType = ASSIGNED) {
      it.assignee = getByReference("other-company-fm-participant")
      it.auditingInformationBuilder.lastModifiedDate = reassignedNewsDate.toEpochMilli()
    }
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                reassignedNewsDate,
                reassignedNewsDate))
  }
}
