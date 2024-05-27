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
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TopicCriticalityEnum
import java.util.UUID

data class Topic(
    override val id: String,
    val version: Long? = null,
    val identifier: UUID? = null,
    val taskId: String,
    val description: String? = null,
    val criticality: TopicCriticalityEnum? = null,
    override val createWithUserId: String
) : UserBasedImport(), ImportObject
