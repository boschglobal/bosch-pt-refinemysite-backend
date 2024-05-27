/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.message.command.api

import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId

data class CreateMessageCommand(
    val identifier: MessageId,
    val content: String?,
    val topicIdentifier: TopicId,
    val projectIdentifier: ProjectId
)

data class DeleteMessageCommand(val identifier: MessageId)
