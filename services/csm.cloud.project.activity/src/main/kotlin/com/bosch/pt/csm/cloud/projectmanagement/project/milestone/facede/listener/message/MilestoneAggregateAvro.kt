/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facede.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.MilestoneTypeEnum
import java.util.UUID

fun MilestoneAggregateAvro.toEntity(projectIdentifier: UUID) =
    Milestone(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        name = getName(),
        type = MilestoneTypeEnum.valueOf(getType().name),
        date = getDate().toLocalDateByMillis(),
        header = getHeader(),
        description = getDescription(),
        projectCraftIdentifier = getCraftIdentifier(),
        workAreaIdentifier = getWorkAreaIdentifier())
