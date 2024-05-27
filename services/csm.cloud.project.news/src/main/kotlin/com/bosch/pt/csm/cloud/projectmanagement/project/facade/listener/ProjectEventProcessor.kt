/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener.LiveUpdateEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import org.springframework.stereotype.Component

@Component
class ProjectEventProcessor(
    private val projectEventToStateProcessor: ProjectEventToStateProcessor,
    private val projectEventToNewsProcessor: ProjectEventToNewsProcessor,
    private val liveUpdateEventProcessor: LiveUpdateEventProcessor,
) : BusinessTransactionAware {

  override fun getProcessorName(): String = "news"

  override fun onNonTransactionalEvent(record: EventRecord) {
    projectEventToStateProcessor.process(record.value)
    projectEventToNewsProcessor.process(record.key, record.value, record.messageDate)
    liveUpdateEventProcessor.process(record.key, record.value, record.messageDate)
  }

  override fun onTransactionFinished(
      transactionStartedRecord: EventRecord,
      events: List<EventRecord>,
      transactionFinishedRecord: EventRecord
  ) {
    if (transactionStartedRecord.value is ProjectImportStartedEventAvro ||
        transactionStartedRecord.value is ProjectCopyStartedEventAvro) {
      events.forEach { (_, value): EventRecord -> projectEventToStateProcessor.process(value) }
    } else {
      events.forEach { (key, value, messageDate): EventRecord ->
        projectEventToStateProcessor.process(value)
        projectEventToNewsProcessor.process(key, value, messageDate)
        liveUpdateEventProcessor.process(key, value, messageDate)
      }
    }
  }
}
