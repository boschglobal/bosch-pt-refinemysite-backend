/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Attachment
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import java.util.UUID

fun AttachmentAvro.buildAttachment(
    auditingInformation: AuditingInformation,
    identifier: UUID,
    topicId: UUID? = null,
    taskId: UUID? = null,
    messageId: UUID? = null,
) =
    Attachment(
        auditingInformation = auditingInformation,
        identifier = identifier,
        fileName = getFileName(),
        fileSize = getFileSize(),
        imageHeight = getHeight(),
        imageWidth = getWidth(),
        captureDate = getCaptureDate()?.toLocalDateTimeByMillis(),
        topicId = topicId,
        taskId = taskId,
        messageId = messageId)
