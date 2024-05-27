/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.toAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify activity")
@SmartSiteSpringBootTest
class ActivityCreatorTest : AbstractIntegrationTest() {

  @Autowired lateinit var activityService: ActivityService

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("cr-user")
  }

  /**
   * After creating a activity for a given aggregate the activityExists() will always return true.
   * with a delete event the activity will be removed and therefore the activityExists() will return
   * false. Before creating an activity the creator will check activityExists() and won't create a
   * activity twice.
   */
  @Test
  fun `are only created once`() {
    eventStreamGenerator.submitTask {
      it.assignee = getByReference("fm-participant")
      it.status = DRAFT
    }

    val taskIdentifier = getByReference("task")
    assertThat(activityExists(taskIdentifier)).isTrue

    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.DELETED)

    assertThat(activityExists(taskIdentifier)).isFalse
  }

  @Test
  fun `creation doesn't break when processed sequence events are received again`() {
    assertThat(repositories.activityRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTask {
            it.assignee = getByReference("fm-participant")
            it.status = DRAFT
          }
          .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "version 1" }
          .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "version 2" }
          .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "version 3" }
          .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = "version 4" }
    }
    assertThat(repositories.activityRepository.findAll()).hasSize(5)

    // Check if activity is created correctly again
    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.UPDATED) {
      it.name = "new activity"
    }
    assertThat(repositories.activityRepository.findAll()).hasSize(6)
  }

  private fun activityExists(taskIdentifier: AggregateIdentifierAvro) =
      activityService.activityExists(taskIdentifier.toAggregateIdentifier())
}
