/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.news.repository

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NewsTest : AbstractNewsTest() {

  @Test
  fun `check number of participants`() {
    val participants =
        participantMappingRepository.findAllByProjectIdentifier(
            UUID.fromString(projectAggregateIdentifier.getIdentifier()))
    assertThat(participants).hasSize(5)
  }

  @Test
  fun `check that no news are created for user that initiated the event`() {
    controller.delete(taskAggregateIdentifier, employeeCsm1)
    eventStreamGenerator.submitTopicG2 {
      it.auditingInformationBuilder.lastModifiedBy = employeeCsm1.user
    }
    assertThat(controller.getDetails(employeeCsm1, taskAggregateIdentifier)).isEmpty()
  }

  @Test
  fun `check that csm 2 is the only recipient in case a task is assigned to csm1`() {
    eventStreamGenerator.submitTask(asReference = "task-2") {
      it.assignee = getByReference("csm-participant-1")
      it.auditingInformationBuilder.lastModifiedBy = employeeCsm1.user
    }
    val task: TaskAggregateAvro = context["task-2"] as TaskAggregateAvro
    assertThat(
            newsRepository.findByContextObjectIn(
                listOf(ObjectIdentifier(task.aggregateIdentifier))))
        .size()
        .isEqualTo(1)
  }

  @Test
  fun `check that csm1, csm2 and cr2 are the only recipients in case a task is assigned to cr1`() {
    eventStreamGenerator.submitTask {
      it.assignee = getByReference("cr-participant-1")
      it.auditingInformationBuilder.lastModifiedBy = employeeCr1.user
    }
    val task: TaskAggregateAvro = context["task"] as TaskAggregateAvro
    assertThat(controller.getDetails(employeeCr1, task.aggregateIdentifier)).isEmpty()
    assertThat(
            newsRepository.findByContextObjectIn(
                listOf(ObjectIdentifier(task.aggregateIdentifier))))
        .size()
        .isEqualTo(3)
  }
}
