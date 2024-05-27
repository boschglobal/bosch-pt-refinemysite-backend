/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.TopicCriticalityEnum
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import java.util.UUID

fun TopicAggregateG2Avro.toEntity(projectIdentifier: UUID) =
    Topic(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        taskIdentifier = getTaskIdentifier(),
        description = getDescription(),
        criticality = TopicCriticalityEnum.valueOf(getCriticality().name))
