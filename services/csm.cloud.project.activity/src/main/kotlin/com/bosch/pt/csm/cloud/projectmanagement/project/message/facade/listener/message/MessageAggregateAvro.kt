/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.message.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import java.util.UUID

fun MessageAggregateAvro.toEntity(taskIdentifier: UUID, projectIdentifier: UUID) =
    Message(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        taskIdentifier = taskIdentifier,
        topicIdentifier = getTopicIdentifier(),
        content = getContent())
