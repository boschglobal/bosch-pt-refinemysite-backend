/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.message.boundary.MessageDeleteService
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentService
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
class TopicDeleteService(
    private val topicRepository: TopicRepository,
    private val topicAttachmentService: TopicAttachmentService,
    private val messageDeleteService: MessageDeleteService
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  fun deletePartitioned(taskIds: List<Long>) {

    val topicIds = topicRepository.getIdsByTaskIdsPartitioned(taskIds)
    messageDeleteService.deletePartitioned(topicIds)
    topicAttachmentService.deletePartitioned(topicIds)
    topicRepository.deletePartitioned(topicIds)
  }
}
