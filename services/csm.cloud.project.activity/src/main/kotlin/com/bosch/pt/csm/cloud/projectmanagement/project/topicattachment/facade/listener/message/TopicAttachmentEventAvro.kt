/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topicattachment.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getLastModifiedDate

fun TopicAttachmentEventAvro.buildEventInformation() =
    EventInformation(
        name = getName().name,
        date = getLastModifiedDate(),
        user = getLastModifiedByUserIdentifier())
