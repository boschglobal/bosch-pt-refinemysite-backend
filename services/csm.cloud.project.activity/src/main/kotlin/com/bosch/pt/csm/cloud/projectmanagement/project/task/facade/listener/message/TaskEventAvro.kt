/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro

fun TaskEventAvro.buildEventInformation() =
    EventInformation(
        name = getName().name,
        date = getLastModifiedDate(),
        user = getLastModifiedByUserIdentifier())
