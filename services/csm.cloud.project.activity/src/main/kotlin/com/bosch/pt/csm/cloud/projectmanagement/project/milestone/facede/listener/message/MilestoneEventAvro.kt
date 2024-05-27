/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facede.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getLastModifiedBy
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro

fun MilestoneEventAvro.buildEventInformation() =
    EventInformation(
        name = getName().name, date = getLastModifiedDate(), user = getLastModifiedBy())
