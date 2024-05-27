/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.boundary.precondition

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_UPDATE_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountAggregation
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.assertCreateTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.assertUpdateTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isAssignTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isCloseTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isDeletePossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isResetTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isStartTaskPossible
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TaskPreconditionTest {

  /** Verifies that a task can be assigned when the status is either OPEN, STARTED or DRAFT. */
  @Test
  fun `verify is assign task possible`() {
    assertThat(isAssignTaskPossible(OPEN)).isTrue
    assertThat(isAssignTaskPossible(STARTED)).isTrue
    assertThat(isAssignTaskPossible(DRAFT)).isTrue
  }

  /** Verifies that a task cannot be assigned when the status is CLOSED or ACCEPTED. */
  @Test
  fun `verify not possible to assign task`() {
    assertThat(isAssignTaskPossible(CLOSED)).isFalse
    assertThat(isAssignTaskPossible(ACCEPTED)).isFalse
  }

  /** Verifies that a task can be started when the status is DRAFT or OPEN. */
  @Test
  fun `verify start task`() {
    assertThat(isStartTaskPossible(DRAFT)).isTrue
    assertThat(isStartTaskPossible(OPEN)).isTrue
  }

  /**
   * Verifies that a task cannot be started when the status is either STARTED, CLOSED or ACCEPTED.
   */
  @Test
  fun `verify not be possible to start task`() {
    assertThat(isStartTaskPossible(STARTED)).isFalse
    assertThat(isStartTaskPossible(CLOSED)).isFalse
    assertThat(isStartTaskPossible(ACCEPTED)).isFalse
  }

  /** Verifies that a task can be closed when the status is DRAFT, OPEN and STARTED. */
  @Test
  fun `verify close task`() {
    assertThat(isCloseTaskPossible(DRAFT)).isTrue
    assertThat(isCloseTaskPossible(OPEN)).isTrue
    assertThat(isCloseTaskPossible(STARTED)).isTrue
  }

  /** Verifies that a task cannot be closed when the status is CLOSED or ACCEPTED. */
  @Test
  fun `verify not be possible to close task`() {
    assertThat(isCloseTaskPossible(CLOSED)).isFalse
    assertThat(isCloseTaskPossible(ACCEPTED)).isFalse
  }

  /** Verifies that a task can be reset when the status is ahead of OPEN. */
  @Test
  fun `verify reset task`() {
    assertThat(isResetTaskPossible(STARTED)).isTrue
    assertThat(isResetTaskPossible(CLOSED)).isTrue
    assertThat(isResetTaskPossible(ACCEPTED)).isTrue
  }

  /** Verifies that a task cannot be reset when the status is DRAFT or OPEN. */
  @Test
  fun `verify reset task not possible`() {
    assertThat(isResetTaskPossible(DRAFT)).isFalse
    assertThat(isResetTaskPossible(OPEN)).isFalse
  }

  /** Verifies that a task can be created when the status is either DRAFT or OPEN with a company. */
  @Test
  fun `verify create task`() {
    assertCreateTaskPossible(DRAFT, null)
    assertCreateTaskPossible(DRAFT, ParticipantId())
    assertCreateTaskPossible(OPEN, ParticipantId())
  }

  /**
   * Verifies that a task cannot be created when the status is either STARTED, CLOSED or OPEN
   * without a company.
   */
  @Test
  fun `verify not be possible to create task`() {
    try {
      assertCreateTaskPossible(STARTED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
    try {
      assertCreateTaskPossible(STARTED, ParticipantId())
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
    try {
      assertCreateTaskPossible(CLOSED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
    try {
      assertCreateTaskPossible(CLOSED, ParticipantId())
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
    try {
      assertCreateTaskPossible(OPEN, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
  }

  /**
   * Verifies that a task can be updated when the status is either STARTED, OPEN, or CLOSED with a
   * company or DRAFT with or without a company.
   */
  @Test
  fun `verify update task`() {
    assertUpdateTaskPossible(DRAFT, DRAFT, null)
    assertUpdateTaskPossible(DRAFT, DRAFT, ParticipantId())
    assertUpdateTaskPossible(DRAFT, STARTED, ParticipantId())
    assertUpdateTaskPossible(DRAFT, OPEN, ParticipantId())
    assertUpdateTaskPossible(DRAFT, CLOSED, ParticipantId())
    assertUpdateTaskPossible(DRAFT, ACCEPTED, ParticipantId())
  }

  /**
   * Verifies that a task cannot be updated in the following situations: - when the new status is
   * OPEN without an assignee, - when the old status is not DRAFT and the new status is DRAFT.
   */
  @Test
  fun `verify not be possible to update task`() {
    try {
      assertUpdateTaskPossible(OPEN, DRAFT, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey).isEqualTo(SERVER_ERROR_BAD_REQUEST)
    }
    try {
      assertUpdateTaskPossible(STARTED, DRAFT, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey).isEqualTo(SERVER_ERROR_BAD_REQUEST)
    }
    try {
      assertUpdateTaskPossible(CLOSED, CLOSED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_UPDATE_FORBIDDEN)
    }
    try {
      assertUpdateTaskPossible(ACCEPTED, ACCEPTED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_UPDATE_FORBIDDEN)
    }
    try {
      assertUpdateTaskPossible(null, CLOSED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED)
    }
    try {
      assertUpdateTaskPossible(null, ACCEPTED, null)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey)
          .isEqualTo(TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED)
    }
  }

  @Test
  fun `verify delete task possible`() {
    // Ensure that the delete is possible when there's no aggregation result
    assertThat(isDeletePossible(null)).isTrue

    // Ensure that the delete is possible the task doesn't have day cards
    assertThat(isDeletePossible(DayCardStatusCountAggregation())).isTrue

    // Ensure that the delete is possible when there are only open day cards available for a task
    assertThat(isDeletePossible(DayCardStatusCountAggregation(DayCardStatusEnum.OPEN, 3L))).isTrue
  }

  @Test
  fun `verify delete task not possible`() {

    // Ensure that delete is not possible when there are day cards with status different than OPEN
    Stream.of(*DayCardStatusEnum.values())
        .filter { status: DayCardStatusEnum -> status !== DayCardStatusEnum.OPEN }
        .forEach { status: DayCardStatusEnum? ->
          // Ensure that delete is not possible if "valid" and "invalid" status values are
          // available
          val aggregation = DayCardStatusCountAggregation(DayCardStatusEnum.OPEN, 3L)
          aggregation.countByStatus[status!!] = 1L
          assertThat(isDeletePossible(aggregation)).isFalse

          // Ensure that delete is not possible if there are only "invalid" status values
          assertThat(isDeletePossible(DayCardStatusCountAggregation(status, 1L))).isFalse()
        }
  }
}
