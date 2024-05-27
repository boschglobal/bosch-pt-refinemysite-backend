/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction

import com.bosch.pt.iot.smartsite.common.businesstransaction.boundary.BusinessTransactionPropagation.REQUIRES_NEW
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BusinessTransactionContextHolderTest {

  @BeforeEach
  @AfterEach
  fun `close all remaining open business transactions`() {
    while (BusinessTransactionContextHolder.isBusinessTransactionActive()) {
      BusinessTransactionContextHolder.closeContext()
    }
  }

  @Test
  fun `after opening a context, the current context is not null`() {
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.getCurrentContext()).isNotNull
  }

  @Test
  fun `after opening a context, the business transaction is active`() {
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.isBusinessTransactionActive()).isTrue
  }

  @Test
  fun `after opening a context, the current business transaction id is not null`() {
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.currentBusinessTransactionId()).isNotNull
  }

  @Test
  fun `after opening a context, the number of open transactions is correct`() {
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.currentlyOpenBusinessTransactions()).isOne

    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.currentlyOpenBusinessTransactions()).isZero
  }

  @Test
  fun `after opening a inner context, the number of open transactions is correct`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.currentlyOpenBusinessTransactions()).isEqualTo(2)

    BusinessTransactionContextHolder.closeContext()
    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.currentlyOpenBusinessTransactions()).isZero
  }

  @Test
  fun `starting an inner context fails for propagation REQUIRES_NEW`() {
    BusinessTransactionContextHolder.openContext()
    assertThatIllegalStateException().isThrownBy {
      BusinessTransactionContextHolder.openContext(propagation = REQUIRES_NEW)
    }
  }

  @Test
  fun `starting an inner context succeeds for propagation REQUIRED`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.openContext()
    assertThat(BusinessTransactionContextHolder.getCurrentContext()).isNotNull

    BusinessTransactionContextHolder.closeContext()
    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.getCurrentContext()).isNull()
  }

  @Test
  fun `after closing a context, the current context is null`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.getCurrentContext()).isNull()
  }

  @Test
  fun `after closing a context, no business transaction is active`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.isBusinessTransactionActive()).isFalse
  }

  @Test
  fun `after closing a context, the current business transaction id is null`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.closeContext()
    assertThat(BusinessTransactionContextHolder.currentBusinessTransactionId()).isNull()
  }

  @Test
  fun `closing a non-existent context fails`() {
    assertThatIllegalStateException().isThrownBy { BusinessTransactionContextHolder.closeContext() }
  }
}
