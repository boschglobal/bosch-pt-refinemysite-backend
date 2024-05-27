/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction.boundary

import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionFinishedMessageKeyAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionStartedMessageKeyAvro
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.currentlyOpenBusinessTransactions
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.isBusinessTransactionActive
import com.bosch.pt.iot.smartsite.common.kafka.eventstore.EventStore
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamable
import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Suppress("UnnecessaryAbstractClass")
abstract class ProducerBusinessTransactionManager(
    private val em: EntityManager,
    private val eventStore: EventStore
) {

  /**
   * Starts a new business transaction or join a currently active business transaction.
   *
   * Saves the given [startEvent] to the [EventStore] only if there is no current business
   * transaction active yet. When starting an inner business transaction, the [startEvent] will be
   * ignored.
   *
   * Note: If this operation fails with an exception, a business transaction context might be left
   * open. Callers are advised to use a try-catch-finally block to close the context in case of an
   * exception. For this, use [BusinessTransactionContextHolder.closeContext].
   *
   * TODO: [SMAR-16826] Make ProducerBusinessTransactionManager more robust
   */
  @Transactional(propagation = MANDATORY)
  open fun startTransaction(
      startEvent: KafkaStreamable,
      propagation: BusinessTransactionPropagation = BusinessTransactionPropagation.REQUIRED
  ) {
    if (propagation == BusinessTransactionPropagation.REQUIRES_NEW) {
      checkNoActiveBusinessTransaction()
    }
    // important: this is the latest point to start the transaction! do not start it later.
    BusinessTransactionContextHolder.openContext()
    val contextAfterStart = BusinessTransactionContextHolder.getCurrentContext()!!

    requireEventTypeIsStarted(startEvent)

    if (contextAfterStart.isOutermost()) {
      // save the start event only if the outermost business transaction was started. For inner
      // transactions, the start event must not be saved because business transactions must have
      // exactly one start event.
      eventStore.save(startEvent)
    }
  }

  /**
   * Finishes the current business transaction.
   *
   * Saves the given [finishEvent] to the [EventStore] only if the current business transaction is
   * the outermost business transaction. When finishing an inner business transaction, the
   * [finishEvent] will be ignored.
   */
  @Transactional(propagation = MANDATORY)
  open fun finishTransaction(finishEvent: KafkaStreamable) {
    checkActiveBusinessTransaction()
    requireEventTypeIsFinished(finishEvent)

    val contextBeforeClose = BusinessTransactionContextHolder.getCurrentContext()!!
    if (contextBeforeClose.isOutermost()) {
      // save the finish event only if the outermost business transaction was closed. For inner
      // transactions, the finish event must not be saved because business transactions must have
      // exactly one finish event.
      em.flush()
      eventStore.save(finishEvent)
    }

    // important: this is the earliest point to finish the transaction! do not finish it sooner.
    BusinessTransactionContextHolder.closeContext()
  }

  @Transactional(propagation = MANDATORY)
  open fun <R> doInBusinessTransaction(
      startEvent: KafkaStreamable,
      finishEvent: KafkaStreamable,
      block: () -> R
  ): R {
    val openBefore = currentlyOpenBusinessTransactions()
    try {
      startTransaction(startEvent)
      val result = block()
      finishTransaction(finishEvent)
      return result
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
      val openAfter = currentlyOpenBusinessTransactions()
      if (openAfter > openBefore) {
        check(openAfter - openBefore == 1)
        // close the context left open due to the exception
        BusinessTransactionContextHolder.closeContext()
      }
      throw e
    } finally {
      check(currentlyOpenBusinessTransactions() == openBefore)
    }
  }

  private fun checkActiveBusinessTransaction() =
      check(isBusinessTransactionActive()) {
        "Tried to finish business transaction but there is no active business transaction."
      }

  private fun checkNoActiveBusinessTransaction() =
      check(!isBusinessTransactionActive()) {
        "Tried to start business transaction but there is already an active business transaction."
      }

  private fun requireEventTypeIsStarted(event: KafkaStreamable) =
      require(event.toMessageKey() is BusinessTransactionStartedMessageKey) {
        "A start event must have a message key of type ${BusinessTransactionStartedMessageKeyAvro::class.java}"
      }

  private fun requireEventTypeIsFinished(event: KafkaStreamable) =
      require(event.toMessageKey() is BusinessTransactionFinishedMessageKey) {
        "A finish event must have a message key of type ${BusinessTransactionFinishedMessageKeyAvro::class.java}"
      }
}

enum class BusinessTransactionPropagation {

  /**
   * Joins an already existing transaction or creates a new one if none exists, yet. Analogous to
   * [org.springframework.transaction.annotation.Propagation.REQUIRED].
   *
   * When a business transaction joins another one, both will have the same transaction identifier.
   * This will not create nested business transactions!
   */
  REQUIRED,

  /**
   * Creates a new business transaction, or fails if a business transaction already exists.
   *
   * Note: This behaviour is inspired by, but slightly different from
   * [org.springframework.transaction.annotation.Propagation.REQUIRES_NEW].
   */
  REQUIRES_NEW
}
