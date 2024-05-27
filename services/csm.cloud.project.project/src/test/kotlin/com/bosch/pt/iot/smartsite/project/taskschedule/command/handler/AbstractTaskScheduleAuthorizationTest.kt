/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTaskScheduleAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  protected val taskWithScheduleIdentifier by lazy {
    EventStreamGeneratorStaticExtensions.getIdentifier("taskWithSchedule").asTaskId()
  }

  protected val taskWithScheduleIdentifier2 by lazy {
    EventStreamGeneratorStaticExtensions.getIdentifier("taskWithSchedule2").asTaskId()
  }

  protected val taskWithoutScheduleIdentifier by lazy {
    EventStreamGeneratorStaticExtensions.getIdentifier("taskWithoutSchedule").asTaskId()
  }

  protected val taskWithoutScheduleIdentifier2 by lazy {
    EventStreamGeneratorStaticExtensions.getIdentifier("taskWithoutSchedule2").asTaskId()
  }

  /**
   * the identifier of a task that belongs to a project without participants, so that no regular
   * user is authorized to access that project
   */
  protected val otherProjectTaskIdentifier by lazy {
    EventStreamGeneratorStaticExtensions.getIdentifier("otherProjectTask").asTaskId()
  }

  protected val taskSchedule by lazy {
    repositories.findTaskScheduleWithDetails(
        EventStreamGeneratorStaticExtensions.getIdentifier("taskSchedule").asTaskScheduleId())!!
  }

  protected val taskSchedule2 by lazy {
    repositories.findTaskScheduleWithDetails(
        EventStreamGeneratorStaticExtensions.getIdentifier("taskSchedule2").asTaskScheduleId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProjectCraftG2()
        .submitTask("taskWithoutSchedule")
        .submitTask("taskWithoutSchedule2")
        .submitTask("taskWithSchedule") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantCreator")
        }
        .submitTaskSchedule("taskSchedule") {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(5).toEpochMilli()
        }
        .submitTask("taskWithSchedule2") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantCreator")
        }
        .submitTaskSchedule("taskSchedule2") {
          it.start = LocalDate.now().plusDays(6).toEpochMilli()
          it.end = LocalDate.now().plusDays(12).toEpochMilli()
        }
        .submitTask(randomString()) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantFm")
        }
        .submitTaskSchedule(randomString())
        .submitProject(randomString())
        .submitTask("otherProjectTask")
        .submitTaskSchedule(randomString())
        .setLastIdentifierForType(
            ProjectmanagementAggregateTypeEnum.PROJECT.value,
            EventStreamGeneratorStaticExtensions.getByReference("project"))
  }
}
