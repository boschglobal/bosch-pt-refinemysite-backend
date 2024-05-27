/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.findLatest
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state and activities")
@SmartSiteSpringBootTest
class CleanupStateFromProjectDeletedEventTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitTask(auditUserReference = "fm-user") {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
      it.status = DRAFT
      // setting all non-mandatory fields to null
      it.location = null
      it.description = null
    }
  }

  @Test
  fun `are cleaned up after project deleted event`() {
    // Check activities and project state are not empty
    requestActivities(task).andExpectOk().andExpect(hasId(findLatestActivity().identifier))

    val project = repositories.projectRepository.findById(project.buildAggregateIdentifier())
    assertThat(project.isPresent).isTrue
    assertThat(repositories.taskRepository.findLatest(task.getIdentifier())).isNotNull

    eventStreamGenerator.submitProject(eventType = DELETED)

    // Check if state is cleaned up and related activities are deleted
    assertThat(repositories.activityRepository.findAll()).isEmpty()
    assertThat(repositories.projectRepository.findAll()).isEmpty()
    assertThat(repositories.taskRepository.findAll()).isEmpty()
  }
}
