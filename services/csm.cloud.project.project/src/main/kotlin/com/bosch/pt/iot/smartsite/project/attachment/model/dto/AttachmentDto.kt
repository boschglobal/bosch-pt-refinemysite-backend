/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.attachment.model.dto

import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.util.Date
import java.util.UUID

data class AttachmentDto
@JvmOverloads
constructor(
    // Attachment information
    val identifier: UUID,
    val version: Long,
    val fileName: String,
    val captureDate: Date? = null,
    val fileSize: Long,
    val imageHeight: Long? = null,
    val imageWidth: Long? = null,
    val fullAvailable: Boolean,
    val smallAvailable: Boolean,
    // Attachment Create User information
    val createdByIdentifier: UUID,
    val createdByFirstName: String? = null,
    val createdByLastName: String? = null,
    val createdDate: Date,
    val createdByDeleted: Boolean,
    // Attachment Modify User information
    val lastModifiedByIdentifier: UUID,
    val lastModifiedByFirstName: String? = null,
    val lastModifiedByLastName: String? = null,
    val lastModifiedDate: Date,
    val lastModifiedByDeleted: Boolean,
    // Task information
    val taskIdentifier: TaskId,
    // Topic information
    val topicIdentifier: TopicId? = null,
    // Message information
    val messageIdentifier: MessageId? = null
)
