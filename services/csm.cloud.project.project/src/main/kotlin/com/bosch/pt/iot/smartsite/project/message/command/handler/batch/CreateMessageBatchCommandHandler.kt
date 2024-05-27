/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.message.command.api.CreateMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.handler.CreateMessageCommandHandler
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateMessageBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createMessageCommandHandler: CreateMessageCommandHandler
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  fun handle(commands: List<CreateMessageCommand>): List<MessageId> {
    if (commands.isEmpty()) {
      return emptyList()
    }

    return businessTransactionManager.doBatchInBusinessTransaction(
        commands.first().projectIdentifier) {
          commands.map { createMessageCommandHandler.handle(it) }
        }
  }
}
