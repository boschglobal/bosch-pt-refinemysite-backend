/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro

fun TaskStatusEnumAvro.asStatus() = TaskStatusEnum.valueOf(this.name)
