/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import java.util.UUID

interface TaskScheduleRepositoryExtension {

    fun find(identifier: UUID, version: Long, projectIdentifier: UUID): TaskSchedule?

    fun deleteTaskSchedule(identifier: UUID, projectIdentifier: UUID)
}
