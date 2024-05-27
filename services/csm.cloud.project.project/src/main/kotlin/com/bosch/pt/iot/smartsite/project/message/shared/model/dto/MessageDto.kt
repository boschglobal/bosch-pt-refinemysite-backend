/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.shared.model.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.util.Date

class MessageDto(
    // Message information
    val identifier: MessageId,
    val version: Long,
    val content: String?,
    // Message Create User information
    val createdBy: UserId,
    val createdDate: Date,
    // Message Modify User information
    val lastModifiedBy: UserId,
    val lastModifiedDate: Date,
    // Topic information
    val topicIdentifier: TopicId
)
