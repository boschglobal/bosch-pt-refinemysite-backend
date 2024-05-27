/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasActivitiesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectNotFound
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.findLatest
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state and activities")
@SmartSiteSpringBootTest
class CleanupStateFromTaskDeletedEventTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.name = "task"
          it.status = DRAFT
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitTask(eventType = UPDATED) { it.description = "first update" }
        .submitTask(eventType = UPDATED) { it.description = "second update" }
  }

  @Test
  fun `are cleaned up after task deleted event`() {
    // Check activities and state are not empty
    assertThat(repositories.taskRepository.findLatest(identifier = task.getIdentifier())).isNotNull

    requestActivities(task, limit = Int.MAX_VALUE)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasActivitiesCount(3))

    eventStreamGenerator.submitTask(eventType = DELETED)

    assertThat(repositories.taskRepository.findLatest(identifier = task.getIdentifier())).isNull()
    requestActivities(task).andExpectNotFound()
  }
}
