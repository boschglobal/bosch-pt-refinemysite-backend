/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.service

import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.MessageRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MessageService(private val messageRepository: MessageRepository) {

  @Trace fun save(message: Message) = messageRepository.save(message)

  @Trace
  fun findMessage(messageIdentifier: UUID, version: Long, projectIdentifier: UUID): Message =
      messageRepository.find(messageIdentifier, version, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      messageRepository.delete(identifier, projectIdentifier)
}
