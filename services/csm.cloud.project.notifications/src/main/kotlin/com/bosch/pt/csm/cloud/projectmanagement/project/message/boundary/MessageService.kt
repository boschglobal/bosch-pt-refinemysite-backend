/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.MessageRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MessageService(private val messageRepository: MessageRepository) {

  @Trace fun save(message: Message): Message = messageRepository.save(message)

  @Trace
  fun deleteMessage(messageIdentifier: UUID, projectIdentifier: UUID) =
      messageRepository.deleteMessage(messageIdentifier, projectIdentifier)
}
