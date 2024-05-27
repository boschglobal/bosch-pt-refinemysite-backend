/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.topic.command.api.CreateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.handler.CreateTopicCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateTopicBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createTopicCommandHandler: CreateTopicCommandHandler
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  fun handle(projectIdentifier: ProjectId, commands: List<CreateTopicCommand>): List<TopicId> {
    if (commands.isEmpty()) {
      return emptyList()
    }

    return businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      commands.map { createTopicCommandHandler.handle(it) }
    }
  }
}
