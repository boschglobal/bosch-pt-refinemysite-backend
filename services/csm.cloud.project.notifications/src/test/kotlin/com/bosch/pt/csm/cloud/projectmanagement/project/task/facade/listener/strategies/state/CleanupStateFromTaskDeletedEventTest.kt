/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify task state")
@SmartSiteSpringBootTest
class CleanupStateFromTaskDeletedEventTest : BaseNotificationStrategyTest() {

  @Test
  fun ` is cleaned up from task deleted event`() {
    eventStreamGenerator.submitTaskAsFm()
    var tasks = repositories.taskRepository.findTasks(projectAggregate.getIdentifier())
    Assertions.assertThat(tasks).hasSize(1)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTask(auditUserReference = FM_USER, eventType = DELETED)
    }

    tasks = repositories.taskRepository.findTasks(projectAggregate.getIdentifier())
    Assertions.assertThat(tasks).isEmpty()
  }
}
