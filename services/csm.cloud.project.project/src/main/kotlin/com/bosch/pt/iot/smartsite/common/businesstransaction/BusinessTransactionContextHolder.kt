/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction

import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation
import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation.REQUIRED
import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation.REQUIRES_NEW
import java.util.UUID
import java.util.UUID.randomUUID

object BusinessTransactionContextHolder {

  private val context: ThreadLocal<BusinessTransactionContext> = ThreadLocal()

  fun openContext(propagation: BusinessTransactionPropagation = REQUIRED) {
    val currentContext = context.get()
    val hasCurrentContext = currentContext != null
    if (propagation == REQUIRES_NEW) {
      check(!hasCurrentContext) {
        "Tried to start business transaction but there is already a business transaction active. " +
            "Use propagation $REQUIRED to support joining an active transaction."
      }
    }
    val newContext =
        if (hasCurrentContext) {
          currentContext.copyWithIncrementedOpenBusinessTransactions()
        } else {
          BusinessTransactionContext()
        }
    context.set(newContext)
  }

  fun closeContext() {
    val currentContext = context.get()
    check(currentContext != null) {
      "Tried to finish business transaction but there is no business transaction active."
    }
    val newContext =
        if (currentContext.isOutermost()) {
          null
        } else {
          currentContext.copyWithDecrementedOpenBusinessTransactions()
        }
    context.set(newContext)
  }

  fun currentBusinessTransactionId(): UUID? = context.get()?.transactionIdentifier

  /**
   * the number of curently open business transactions in this context. Must be greater than or
   * equal to one. A number of two indicates that an inner business transaction was started in the
   * context of an outer business transaction (and that both are not yet finished).
   */
  fun currentlyOpenBusinessTransactions(): Int = getCurrentContext()?.openBusinessTransactions ?: 0

  fun isBusinessTransactionActive(): Boolean = context.get() != null

  fun getCurrentContext(): BusinessTransactionContext? = context.get()
}

data class BusinessTransactionContext(
    val transactionIdentifier: UUID = randomUUID(),
    val openBusinessTransactions: Int = 1
) {

  init {
    require(openBusinessTransactions >= 1)
  }

  fun copyWithIncrementedOpenBusinessTransactions(): BusinessTransactionContext =
      this.copy(openBusinessTransactions = openBusinessTransactions + 1)

  fun copyWithDecrementedOpenBusinessTransactions(): BusinessTransactionContext =
      this.copy(openBusinessTransactions = openBusinessTransactions - 1)

  fun isOutermost() = openBusinessTransactions == 1
}
