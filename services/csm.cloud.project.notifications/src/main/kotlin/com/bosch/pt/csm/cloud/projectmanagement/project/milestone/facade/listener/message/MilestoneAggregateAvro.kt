/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.MilestoneTypeEnum

fun MilestoneAggregateAvro.buildMilestone() =
    Milestone(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getProjectIdentifier(),
        name = name,
        description = description,
        type = MilestoneTypeEnum.valueOf(type.name),
        date = date.toLocalDateByMillis(),
        header = header,
        craftIdentifier = craft?.let { getCraftIdentifier() },
        workAreaIdentifier = workarea?.let { getWorkAreaIdentifier() })
