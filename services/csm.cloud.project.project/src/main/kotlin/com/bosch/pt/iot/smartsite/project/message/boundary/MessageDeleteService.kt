/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentService
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
class MessageDeleteService(
    private val messageRepository: MessageRepository,
    private val messageAttachmentService: MessageAttachmentService
) {

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  fun deletePartitioned(topicIds: List<Long>) {
    val messageIds = messageRepository.getIdsByTopicIdsPartitioned(topicIds)
    messageAttachmentService.deletePartitioned(messageIds)
    messageRepository.deletePartitioned(messageIds)
  }
}
