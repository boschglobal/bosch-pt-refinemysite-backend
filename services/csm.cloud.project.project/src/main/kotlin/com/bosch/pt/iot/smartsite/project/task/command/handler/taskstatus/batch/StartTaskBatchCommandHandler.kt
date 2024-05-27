/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.StartTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class StartTaskBatchCommandHandler(
    private val startTaskCommandHandler: StartTaskCommandHandler,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val snapshotStore: TaskSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(projectIdentifier: ProjectId, identifiers: List<TaskId>) {
    if (identifiers.isEmpty()) return

    // Populate snapshot cache
    snapshotStore.findAllOrIgnore(identifiers)

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      identifiers.map { startTaskCommandHandler.handle(it) }
    }
  }
}
