/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.extension

import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.buildRandomSpecificRecord
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.mockRecord
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BusinessTransactionExtensionsTest {

  @Test
  fun `isBusinessTransactionFinished is true for finished event`() {
    val transactionIdentifier = randomUUID()
    val key = BusinessTransactionFinishedMessageKey(transactionIdentifier, randomUUID())
    val record = mockRecord(0, key, buildRandomSpecificRecord(), transactionIdentifier)

    val finished = record.isBusinessTransactionFinished()

    assertThat(finished).isTrue
  }

  @Test
  fun `isBusinessTransactionFinished is false for started event`() {
    val transactionIdentifier = randomUUID()
    val key = BusinessTransactionStartedMessageKey(transactionIdentifier, randomUUID())
    val record = mockRecord(0, key, buildRandomSpecificRecord(), transactionIdentifier)

    val finished = record.isBusinessTransactionFinished()

    assertThat(finished).isFalse
  }

  @Test
  fun `isBusinessTransactionStarted is true for started event`() {
    val transactionIdentifier = randomUUID()
    val key = BusinessTransactionStartedMessageKey(transactionIdentifier, randomUUID())
    val record = mockRecord(0, key, buildRandomSpecificRecord(), transactionIdentifier)

    val finished = record.isBusinessTransactionStarted()

    assertThat(finished).isTrue
  }

  @Test
  fun `isBusinessTransactionStarted is false for finished event`() {
    val transactionIdentifier = randomUUID()
    val key = BusinessTransactionFinishedMessageKey(transactionIdentifier, randomUUID())
    val record = mockRecord(0, key, buildRandomSpecificRecord(), transactionIdentifier)

    val finished = record.isBusinessTransactionStarted()

    assertThat(finished).isFalse
  }
}
