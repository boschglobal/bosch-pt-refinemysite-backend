/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.authorization

import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class TaskScheduleAuthorizationComponent(
    val taskScheduleRepository: TaskScheduleRepository,
    val taskAuthorizationComponent: TaskAuthorizationComponent
) {
  open fun hasReadPermissionOnTaskSchedules(
      taskScheduleIdentifiers: Collection<TaskScheduleId>
  ): Boolean =
      taskScheduleRepository.findTaskIdentifiersByIdentifierIn(taskScheduleIdentifiers).let {
        it.isNotEmpty() && taskAuthorizationComponent.hasViewPermissionOnTasks(it.toSet())
      }
}
