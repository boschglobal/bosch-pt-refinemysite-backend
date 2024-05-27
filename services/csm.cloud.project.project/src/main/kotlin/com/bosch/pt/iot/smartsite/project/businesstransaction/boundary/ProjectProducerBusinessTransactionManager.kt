/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.businesstransaction.boundary

import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.ProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.common.kafka.eventstore.EventStore
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.BatchOperationFinishedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.BatchOperationStartedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectCopyFinishedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectCopyStartedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectImportFinishedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectImportStartedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectRescheduleFinishedEvent
import com.bosch.pt.iot.smartsite.project.businesstransaction.event.ProjectRescheduleStartedEvent
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Component
open class ProjectProducerBusinessTransactionManager(em: EntityManager, eventStore: EventStore) :
    ProducerBusinessTransactionManager(em, eventStore) {

  @Transactional(propagation = MANDATORY)
  open fun <R> doBatchInBusinessTransaction(projectIdentifier: ProjectId, batch: () -> R): R =
      doInBusinessTransaction(
          startEvent = BatchOperationStartedEvent(projectIdentifier),
          finishEvent = BatchOperationFinishedEvent(projectIdentifier),
          block = batch)

  @Transactional(propagation = MANDATORY)
  open fun <R> doImportInBusinessTransaction(projectIdentifier: ProjectId, import: () -> R): R =
      doInBusinessTransaction(
          startEvent = ProjectImportStartedEvent(projectIdentifier),
          finishEvent = ProjectImportFinishedEvent(projectIdentifier),
          block = import)

  @Transactional(propagation = MANDATORY)
  open fun <R> doCopyProjectInBusinessTransaction(projectIdentifier: ProjectId, copy: () -> R): R =
      doInBusinessTransaction(
          startEvent = ProjectCopyStartedEvent(projectIdentifier),
          finishEvent = ProjectCopyFinishedEvent(projectIdentifier),
          block = copy)

  @Transactional(propagation = MANDATORY)
  open fun <R> doProjectRescheduleInBusinessTransaction(
      projectIdentifier: ProjectId,
      shiftDays: Long,
      reschedule: () -> R
  ): R =
      doInBusinessTransaction(
          startEvent = ProjectRescheduleStartedEvent(projectIdentifier, shiftDays),
          finishEvent = ProjectRescheduleFinishedEvent(projectIdentifier),
          block = reschedule)
}
