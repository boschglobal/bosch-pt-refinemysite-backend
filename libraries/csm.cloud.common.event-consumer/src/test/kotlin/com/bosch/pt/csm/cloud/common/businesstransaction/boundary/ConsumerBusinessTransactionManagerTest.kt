/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.boundary

import com.bosch.pt.csm.cloud.application.MongoDbTest
import com.bosch.pt.csm.cloud.application.MySqlTest
import com.bosch.pt.csm.cloud.application.TestApplication
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.buildRandomMessageKey
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.buildRandomSpecificRecord
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.mockRecord
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ContextConfiguration(classes = [TestApplication::class])
@MySqlTest
internal class JpaConsumerBusinessTransactionManagerTest : ConsumerBusinessTransactionManagerTest()

@SpringBootTest
@TestPropertySource(properties = ["custom.business-transaction.consumer.persistence=mongodb"])
@ContextConfiguration(classes = [TestApplication::class])
@MongoDbTest
internal class MongodbConsumerBusinessTransactionManagerTest :
    ConsumerBusinessTransactionManagerTest()

@Suppress("UnnecessaryAbstractClass")
abstract class ConsumerBusinessTransactionManagerTest {

  @Autowired private lateinit var cut: ConsumerBusinessTransactionManager

  @Autowired
  private lateinit var businessTransactionRepository: EventOfBusinessTransactionRepositoryPort

  @AfterEach fun cleanUp() = businessTransactionRepository.deleteAll()

  @Test
  fun `saving tombstone message throws exceptions`() {
    val record = mockRecord(0, buildRandomMessageKey(), null, randomUUID())

    assertThrows(IllegalArgumentException::class.java) {
      cut.saveEventToDatabase(record, "test-processor")
    }
  }

  @Test
  fun `reading queued events from database returns saved events`() {
    val transactionIdentifier = randomUUID()

    val (expectedKey1, expectedValue1, _) =
        saveRandomRecord(0, "test-processor", transactionIdentifier)
    val (expectedKey2, expectedValue2, _) =
        saveRandomRecord(1, "test-processor", transactionIdentifier)

    val events = cut.readEventsFromDatabase(transactionIdentifier, "test-processor")
    assertThat(events).hasSize(2)

    val (actualKey1, actualValue1) = events[0]
    val (actualKey2, actualValue2) = events[1]

    assertThat(actualKey1).isEqualTo(expectedKey1)
    assertThat(actualValue1).isEqualTo(expectedValue1)
    assertThat(actualKey2).isEqualTo(expectedKey2)
    assertThat(actualValue2).isEqualTo(expectedValue2)
  }

  @Test
  fun `reading queued events from database removes duplicate events for the same offset`() {
    val transactionIdentifier = randomUUID()

    val (expectedKey1, expectedValue1, _) =
        saveRandomRecord(0, "test-processor", transactionIdentifier)
    val (expectedKey2, expectedValue2, _) =
        saveRandomRecord(1, "test-processor", transactionIdentifier)
    saveRandomRecord(1, "test-processor", transactionIdentifier)

    val events = cut.readEventsFromDatabase(transactionIdentifier, "test-processor")
    assertThat(events).hasSize(2)

    val (actualKey1, actualValue1) = events[0]
    val (actualKey2, actualValue2) = events[1]

    assertThat(actualKey1).isEqualTo(expectedKey1)
    assertThat(actualValue1).isEqualTo(expectedValue1)
    assertThat(actualKey2).isEqualTo(expectedKey2)
    assertThat(actualValue2).isEqualTo(expectedValue2)
  }

  @Test
  fun `reading queued events from database returns events only from desired event processor`() {
    val (expectedKey, expectedValue, transactionIdentifier) = saveRandomRecord(0, "test-processor")

    // save some more records with different event processor names.
    // None of these should appear in the returned list of queued events.
    saveRandomRecord(0, "another-test-processor")
    saveRandomRecord(1, "yet-another-test-processor")

    val events = cut.readEventsFromDatabase(transactionIdentifier, "test-processor")
    assertThat(events).hasSize(1)

    val (actualKey, actualValue) = events[0]
    assertThat(actualKey).isEqualTo(expectedKey)
    assertThat(actualValue).isEqualTo(expectedValue)
  }

  @Test
  fun `reading queued events returns none after removing events`() {
    val transactionIdentifier = randomUUID()
    saveRandomRecord(0, "test-processor", transactionIdentifier)
    saveRandomRecord(1, "test-processor", transactionIdentifier)

    cut.removeEventsFromDatabase(transactionIdentifier, "test-processor")

    val events = cut.readEventsFromDatabase(transactionIdentifier, "test-processor")
    assertThat(events).isEmpty()
  }

  private fun saveRandomRecord(
      offset: Long,
      eventProcessorName: String,
      transactionIdentifier: UUID = randomUUID()
  ): Triple<AggregateEventMessageKey, SpecificRecordBase, UUID> {
    val expectedKey = buildRandomMessageKey()
    val expectedValue = buildRandomSpecificRecord()

    val record = mockRecord(offset, expectedKey, expectedValue, transactionIdentifier)

    cut.saveEventToDatabase(record, eventProcessorName)

    return Triple(expectedKey, expectedValue, transactionIdentifier)
  }
}
