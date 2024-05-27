/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.shared.model.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import java.util.Date

class TopicDto(
    val identifier: TopicId,
    val version: Long,
    val criticality: TopicCriticalityEnum,
    val description: String?,
    val deleted: Boolean,
    val createdBy: UserId,
    val createdDate: Date,
    val lastModifiedBy: UserId,
    val lastModifiedDate: Date,
    val taskIdentifier: TaskId
)
