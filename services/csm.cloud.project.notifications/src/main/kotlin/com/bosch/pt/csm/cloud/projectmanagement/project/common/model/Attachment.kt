/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.model

import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro

data class Attachment(
    val fullAvailable: Boolean = false,
    val smallAvailable: Boolean = false,
    val width: Long = 0,
    val height: Long = 0,
    val fileSize: Long = 0,
    val fileName: String? = null,
    val captureDate: Long? = null
) {
    companion object {

        fun fromAttachmentAvro(attachmentAvro: AttachmentAvro) = Attachment(
            fullAvailable = attachmentAvro.getFullAvailable(),
            smallAvailable = attachmentAvro.getSmallAvailable(),
            width = attachmentAvro.getWidth(),
            height = attachmentAvro.getHeight(),
            fileSize = attachmentAvro.getFileSize(),
            fileName = attachmentAvro.getFileName(),
            captureDate = attachmentAvro.getCaptureDate()
        )
    }
}
