/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message

import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro

fun MessageAttachmentEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun MessageAttachmentEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun MessageAttachmentEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun MessageAttachmentEventAvro.getMessageIdentifier() = getAggregate().getMessageIdentifier()

fun MessageAttachmentEventAvro.getMessageVersion() = getAggregate().getMessageVersion()
