/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro

fun DayCardEventG2Avro.getDisplayName(): String = getAggregate().getTitle()

fun DayCardEventG2Avro.buildEventInformation() =
    EventInformation(
        name = getName().name,
        date = getLastModifiedDate(),
        user = getLastModifiedByUserIdentifier())
