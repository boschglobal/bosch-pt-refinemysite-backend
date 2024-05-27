/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.UnassignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UnassignTaskBatchCommandHandler(
    private val unassignTaskCommandHandler: UnassignTaskCommandHandler,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(
      projectIdentifier: ProjectId,
      identifiers: List<TaskId>,
  ) {
    if (identifiers.isEmpty()) return

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      identifiers.map { unassignTaskCommandHandler.handle(it) }
    }
  }
}
