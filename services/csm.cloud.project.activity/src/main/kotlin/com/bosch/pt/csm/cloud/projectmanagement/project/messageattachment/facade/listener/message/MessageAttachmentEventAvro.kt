/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.messageattachment.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.getLastModifiedDate

fun MessageAttachmentEventAvro.buildEventInformation() =
    EventInformation(
        name = getName().name,
        date = getLastModifiedDate(),
        user = getLastModifiedByUserIdentifier())
