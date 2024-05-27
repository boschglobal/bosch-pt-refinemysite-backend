/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro

fun TaskConstraintCustomizationAggregateAvro.toEntity() =
    TaskConstraintCustomization(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getProjectIdentifier(),
        TaskConstraintEnum.valueOf(getKey().name),
        getActive(),
        getName())
