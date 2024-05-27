/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro

fun MessageAttachmentAggregateAvro.getIdentifier() =
    getAggregateIdentifier().getIdentifier().toUUID()

fun MessageAttachmentAggregateAvro.getMessageIdentifier() = getMessage().getIdentifier().toUUID()

fun MessageAttachmentAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun MessageAttachmentAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun MessageAttachmentAggregateAvro.getMessageVersion() = getMessage().getVersion()

fun MessageAttachmentAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
