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

class TopicWithMessageCountDto(val topic: TopicDto, val messageCount: Long) {
  val identifier: TopicId
    get() = topic.identifier

  val version: Long
    get() = topic.version

  val taskIdentifier: TaskId
    get() = topic.taskIdentifier

  val createdByIdentifier: UserId
    get() = topic.createdBy

  val lastModifiedByIdentifier: UserId
    get() = topic.lastModifiedBy

  val createdDate: Date
    get() = topic.createdDate

  val lastModifiedDate: Date
    get() = topic.lastModifiedDate

  val criticality: TopicCriticalityEnum
    get() = topic.criticality

  val description: String?
    get() = topic.description
}
