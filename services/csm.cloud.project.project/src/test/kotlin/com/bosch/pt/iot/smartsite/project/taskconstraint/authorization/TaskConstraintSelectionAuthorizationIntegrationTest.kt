/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.authorization

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.INFORMATION
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskConstraintSelectionAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskConstraintSelectionService

  private val taskWithoutConstraintsIdentifier by lazy {
    getIdentifier("taskWithoutConstraints").asTaskId()
  }
  private val taskWithConstraintsIdentifier by lazy {
    getIdentifier("taskWithConstraints").asTaskId()
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProjectCraftG2()
        .submitTask("taskWithoutConstraints") { it.assignee = getByReference("participantCreator") }
        .submitTask("taskWithConstraints") { it.assignee = getByReference("participantCreator") }
        .submitTaskAction { it.actions = listOf(TaskActionEnumAvro.INFORMATION) }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify create selection is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.createEmptySelectionIfNotExists(
          taskWithoutConstraintsIdentifier, setOf(COMMON_UNDERSTANDING))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify create selection for unknown task is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.createEmptySelectionIfNotExists(TaskId(), setOf(COMMON_UNDERSTANDING))
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify update selection is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.updateSelection(
          project.identifier,
          taskWithConstraintsIdentifier,
          setOf(COMMON_UNDERSTANDING, INFORMATION),
          ETag.from(0L))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update selection for unknown task is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.updateSelection(project.identifier, TaskId(), setOf(COMMON_UNDERSTANDING), ETag.from(0L))
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find selections for multiple tasks is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findSelections(setOf(taskWithConstraintsIdentifier)) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find selections for multiple tasks if one is unknown is denied for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findSelections(setOf(taskWithConstraintsIdentifier, TaskId())) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find selection for a task is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findSelection(taskWithConstraintsIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find selection for unknown task is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findSelection(TaskId()) }
  }
}
