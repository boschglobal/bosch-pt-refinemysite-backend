/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import datadog.trace.api.Trace
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TaskScheduleResourceFactoryHelper(
    messageSource: MessageSource,
    private val userService: UserService
) : AbstractResourceFactoryHelper(messageSource) {

  @Trace
  @Transactional(readOnly = true)
  open fun build(
      schedules: Collection<TaskSchedule>,
  ): List<TaskScheduleResource> {

    val auditUsers = userService.findAuditUsers(schedules)

    return schedules.map { buildTaskScheduleResource(it, auditUsers) }
  }

  private fun buildTaskScheduleResource(
      schedule: TaskSchedule,
      auditUsers: Map<UserId, User>
  ): TaskScheduleResource {

    // Create task resource references
    val taskResourceReference =
        ResourceReference(schedule.task.identifier.toUuid(), schedule.task.getDisplayName())

    return TaskScheduleResource(
        id = schedule.identifier.toUuid(),
        version = schedule.version,
        createdDate = schedule.createdDate.get().toDate(),
        createdBy = referTo(auditUsers[schedule.createdBy.get()]!!, deletedUserReference)!!,
        lastModifiedDate = schedule.lastModifiedDate.get().toDate(),
        lastModifiedBy =
            referTo(auditUsers[schedule.lastModifiedBy.get()]!!, deletedUserReference)!!,
        start = schedule.start,
        end = schedule.end,
        task = taskResourceReference,
        slots = null)
  }
}
