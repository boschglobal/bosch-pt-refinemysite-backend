/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.attachment.api.request

import java.io.File
import java.time.ZoneOffset
import java.util.UUID

data class CreateAttachmentResource(
    val id: UUID? = null,
    val taskId: UUID? = null,
    val topicId: UUID? = null,
    val zoneOffset: ZoneOffset? = null,
    val file: File? = null
)
