/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro

fun TaskActionEnumAvro.asTaskConstraintEnum() = TaskConstraintEnum.valueOf(this.name)
