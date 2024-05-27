/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.query

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskScheduleQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskScheduleQueryService

  private val taskWithScheduleIdentifier by lazy { getIdentifier("taskWithSchedule").asTaskId() }

  private val taskWithScheduleIdentifier2 by lazy { getIdentifier("taskWithSchedule2").asTaskId() }

  private val taskScheduleIdentifier by lazy { getIdentifier("taskSchedule").asTaskScheduleId() }

  private val taskScheduleIdentifier2 by lazy { getIdentifier("taskSchedule2").asTaskScheduleId() }

  /**
   * the identifiers that belong to a project without participants, so that no regular user is
   * authorized to access that project
   */
  private val projectWithoutParticipantsIdentifier by lazy {
    getIdentifier("projectWithoutParticipants").asProjectId()
  }

  private val projectWithoutParticipantsTaskIdentifier by lazy {
    getIdentifier("projectWithoutParticipantsTask")
  }

  private val projectWithoutParticipantsTaskScheduleIdentifier by lazy {
    getIdentifier("projectWithoutParticipantsTaskSchedule").asTaskScheduleId()
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProjectCraftG2()
        .submitTask("taskWithSchedule") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("participantCreator")
        }
        .submitTaskSchedule("taskSchedule") {
          it.start = now().toEpochMilli()
          it.end = now().plusDays(5).toEpochMilli()
        }
        .submitTask("taskWithSchedule2") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("participantCreator")
        }
        .submitTaskSchedule("taskSchedule2")
        .submitProject("projectWithoutParticipants")
        .submitTask("projectWithoutParticipantsTask")
        .submitTaskSchedule("projectWithoutParticipantsTaskSchedule")
        .setLastIdentifierForType(PROJECT.value, getByReference("project"))
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.find(taskScheduleIdentifier, project.identifier)).isNotNull
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find permission is denied for non-existing task schedule`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.find(TaskScheduleId(), ProjectId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find permission is denied for project which is not participant`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.find(
          projectWithoutParticipantsTaskScheduleIdentifier, projectWithoutParticipantsIdentifier)
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find by task permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskWithScheduleIdentifier))
          .isNotNull
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by task permission is denied for non-existing task`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findTaskScheduleWithDayCardsDtoByTaskIdentifier(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by task permission is denied for project which is not participant`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      cut.findTaskScheduleWithDayCardsDtoByTaskIdentifier(
          projectWithoutParticipantsTaskIdentifier.asTaskId())
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find by tasks permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(
              cut.findByTaskIdentifiers(
                  listOf(taskWithScheduleIdentifier, taskWithScheduleIdentifier2)))
          .isNotNull
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by tasks permission is denied for non-existing task`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.findByTaskIdentifiers(listOf(taskWithScheduleIdentifier, TaskId()))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by tasks permission is denied for task which is not participant`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      cut.findByTaskIdentifiers(
          listOf(taskWithScheduleIdentifier, projectWithoutParticipantsTaskIdentifier.asTaskId()))
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `find by task schedules permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(
              cut.findByTaskScheduleIdentifiers(
                  setOf(taskScheduleIdentifier, taskScheduleIdentifier2)))
          .isNotNull
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by task schedules permission is denied for non-existing task`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findByTaskIdentifiers(listOf(TaskId(), TaskId())) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `find by task schedules permission is denied for task schedule which is not participant`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findByTaskIdentifiers(listOf(TaskId(), TaskId())) }
  }
}
