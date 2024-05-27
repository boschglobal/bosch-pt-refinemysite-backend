/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message

import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro

fun TopicAttachmentEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun TopicAttachmentEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TopicAttachmentEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun TopicAttachmentEventAvro.getTopicIdentifier() = getAggregate().getTopicIdentifier()

fun TopicAttachmentEventAvro.getTopicVersion() = getAggregate().getTopicVersion()
