/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.common.matchesNewsInAnyOrder
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TaskUnassignNewsTest : AbstractNewsTest() {

  @Test
  fun `news for the previous assignee is deleted`() {
    eventStreamGenerator.submitTask(eventType = UNASSIGNED) { it.assignee = null }
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `a second unassignment is idempotent`() {
    repeat(2) { eventStreamGenerator.submitTask(eventType = UNASSIGNED) { it.assignee = null } }
    assertThat(controller.getDetails(employeeFm, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `news still exist for another construction site manager`() {
    val reassignedNewsDate = Instant.now().plusSeconds(3)
    eventStreamGenerator.submitTask(eventType = UNASSIGNED) {
      it.assignee = null
      it.auditingInformationBuilder.lastModifiedDate = reassignedNewsDate.toEpochMilli()
    }
    assertThat(controller.getDetails({ employeeCsm2 }, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                lastModifiedDate = reassignedNewsDate))
  }

  @Test
  fun `news is still created for another construction site manager`() {
    val reassignedNewsDate = Instant.now().plusSeconds(3)
    controller.delete(taskAggregateIdentifier) { employeeCsm2 }
    eventStreamGenerator.submitTask(eventType = UNASSIGNED) {
      it.assignee = null
      it.auditingInformationBuilder.lastModifiedDate = reassignedNewsDate.toEpochMilli()
    }
    assertThat(controller.getDetails({ employeeCsm2 }, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier,
                reassignedNewsDate,
                reassignedNewsDate))
  }
}
