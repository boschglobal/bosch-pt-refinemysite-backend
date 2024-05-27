/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.model

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum

class TaskStatusStatisticsEntry(
    count: Long,
    property: TaskStatusEnum,
    entityIdentifier: ProjectId
) : StatisticsEntry<TaskStatusEnum, Long>(count, property, entityIdentifier)
