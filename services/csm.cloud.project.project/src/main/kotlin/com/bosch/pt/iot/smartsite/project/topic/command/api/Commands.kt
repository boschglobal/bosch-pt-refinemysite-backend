/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.topic.command.api

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum

data class CreateTopicCommand(
    val identifier: TopicId,
    val criticality: TopicCriticalityEnum,
    val description: String? = null,
    val taskIdentifier: TaskId,
    val projectIdentifier: ProjectId
)

data class EscalateTopicCommand(val identifier: TopicId)

data class DeescalateTopicCommand(val identifier: TopicId)

data class DeleteTopicCommand(val identifier: TopicId)
