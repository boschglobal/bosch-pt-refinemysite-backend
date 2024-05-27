/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.dto

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId

/** this class is meant to be used in a Spring Data dynamic projection (Spring Data DTO) */
data class TaskIdDto(val identifier: TaskId)
