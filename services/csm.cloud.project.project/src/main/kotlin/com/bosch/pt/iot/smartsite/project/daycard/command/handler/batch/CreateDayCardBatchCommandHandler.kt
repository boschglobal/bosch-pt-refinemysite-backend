/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.CreateDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateDayCardBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val projectRepository: ProjectRepository,
    private val createDayCardCommandHandler: CreateDayCardCommandHandler
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(commands: List<CreateDayCardCommand>, projectIdentifier: ProjectId) {
    if (commands.isEmpty()) {
      return
    }

    // Populate existence cache
    projectRepository.findAllByIdentifierIn(listOf(projectIdentifier))

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      commands.map { createDayCardCommandHandler.handle(it) }
    }
  }
}
