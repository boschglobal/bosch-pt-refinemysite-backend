/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.precondition

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_UPDATE_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountAggregation
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED

object TaskPrecondition {

  fun isAssignTaskPossible(status: TaskStatusEnum): Boolean = status !in setOf(CLOSED, ACCEPTED)

  fun isUnassignTaskPossible(status: TaskStatusEnum): Boolean = status !in setOf(CLOSED, ACCEPTED)

  fun isSendTaskPossible(status: TaskStatusEnum): Boolean = status === DRAFT

  fun isStartTaskPossible(status: TaskStatusEnum): Boolean = status < STARTED

  fun isCloseTaskPossible(status: TaskStatusEnum): Boolean = status < CLOSED

  fun isAcceptTaskPossible(status: TaskStatusEnum): Boolean = status !== ACCEPTED

  fun isResetTaskPossible(status: TaskStatusEnum): Boolean = status > OPEN

  fun isDeletePossible(aggregation: DayCardStatusCountAggregation?): Boolean =
      if (aggregation == null || aggregation.countByStatus.isEmpty()) {
        true
      } else {
        aggregation.countByStatus.size == 1 &&
            aggregation.countByStatus[DayCardStatusEnum.OPEN] != null
      }

  fun isReschedulablePossible(status: TaskStatusEnum): Boolean = status !in setOf(CLOSED, ACCEPTED)

  fun assertCreateTaskPossible(status: TaskStatusEnum, assigneeIdentifier: ParticipantId?) {
    if (!(status === DRAFT || status === OPEN && assigneeIdentifier != null)) {
      throw PreconditionViolationException(
          TASK_VALIDATION_ERROR_CREATION_POSSIBLE_WHEN_STATUS_DRAFT_OR_COMPANY_ASSIGNED)
    }
  }

  @Suppress("ThrowsCount")
  fun assertUpdateTaskPossible(
      oldStatus: TaskStatusEnum?,
      newStatus: TaskStatusEnum,
      assigneeId: ParticipantId?
  ) {
    if (oldStatus === CLOSED || oldStatus === ACCEPTED) {
      throw PreconditionViolationException(
          TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_UPDATE_FORBIDDEN)
    } else if (oldStatus !== DRAFT && newStatus === DRAFT) {
      throw PreconditionViolationException(SERVER_ERROR_BAD_REQUEST)
    } else if ((newStatus === CLOSED || newStatus === ACCEPTED) && assigneeId == null) {
      throw PreconditionViolationException(
          TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED)
    }
  }
}
