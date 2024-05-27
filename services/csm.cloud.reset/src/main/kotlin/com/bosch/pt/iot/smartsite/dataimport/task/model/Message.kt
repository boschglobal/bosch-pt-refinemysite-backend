/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport
import java.util.UUID

class Message(
    override val id: String,
    val version: Long? = null,
    val identifier: UUID? = null,
    val topicId: String,
    val content: String? = null
) : UserBasedImport(), ImportObject
