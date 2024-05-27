/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro

fun TopicEventG2Avro.buildEventInformation() =
    EventInformation(
        name = getName().name,
        date = getLastModifiedDate(),
        user = getLastModifiedByUserIdentifier())
