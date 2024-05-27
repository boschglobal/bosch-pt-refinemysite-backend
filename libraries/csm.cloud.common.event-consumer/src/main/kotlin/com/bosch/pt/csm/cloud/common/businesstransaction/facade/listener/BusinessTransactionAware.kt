/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord

/**
 * Use this interface in combination with [AbstractBusinessTransactionAwareListener] to implement a
 * business transaction aware event processor.
 */
interface BusinessTransactionAware {

  /**
   * the name of this business-transaction-aware event processor. This name must be unique across
   * all event processors.
   */
  fun getProcessorName(): String

  /**
   * Invoked for any "transaction started" event.
   *
   * @param transactionStartedRecord the record containing the "transaction started" event
   */
  fun onTransactionStarted(transactionStartedRecord: EventRecord) {
    // implement if needed
  }

  /**
   * Invoked for any "transaction finished" event.
   *
   * @param transactionStartedRecord the record containing the "transaction started" event
   * @param events the sequence of events that happend in between the "transaction started" and "
   * transaction finished" event of the business transaction, in order of their occurence. Does not
   * contain the "transaction started" event nor the "transaction finished" event.
   * @param transactionFinishedRecord the record containing the "transaction finished" event
   */
  fun onTransactionFinished(
      transactionStartedRecord: EventRecord,
      events: List<EventRecord>,
      transactionFinishedRecord: EventRecord
  ) {
    // implement if needed
  }

  /**
   * Invoked for any event that was produced in the scope of a business transaction. This method is
   * not invoked for a "transaction started" or "transaction finished" event.
   */
  fun onTransactionalEvent(record: EventRecord) {
    // implement if needed
  }

  /** Invoked for any event that is not part of a business transaction. */
  fun onNonTransactionalEvent(record: EventRecord) {
    // implement if needed
  }
}
