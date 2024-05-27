/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.attachment.api.response

import com.bosch.pt.iot.smartsite.dataimport.attachment.model.ImageMetadata
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_links")
data class AttachmentResource(
    val id: UUID? = null,
    val taskId: UUID? = null,
    val topicId: UUID? = null,
    val messageId: UUID? = null,
    val projectId: UUID? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val imageMetadata: ImageMetadata? = null
) : AuditableResource()
