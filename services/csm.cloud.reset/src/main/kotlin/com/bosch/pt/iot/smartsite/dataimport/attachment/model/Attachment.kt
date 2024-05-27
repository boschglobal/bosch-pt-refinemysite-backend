/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.attachment.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport
import org.springframework.core.io.Resource

data class Attachment(
    override val id: String,
    val taskId: String? = null,
    val topicId: String? = null,
    val messageId: String? = null,
    val projectId: String? = null,
    val userId: String? = null,
    val path: String,
    val zoneOffset: String? = null,
    val version: Long? = null,
    var resource: Resource? = null
) : UserBasedImport(), ImportObject
