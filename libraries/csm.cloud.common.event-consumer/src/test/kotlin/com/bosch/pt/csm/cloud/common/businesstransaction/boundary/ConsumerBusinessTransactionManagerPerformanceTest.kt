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
import java.util.Random
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ContextConfiguration(classes = [TestApplication::class])
@MySqlTest
internal class JpaConsumerBusinessTransactionManagerPerformanceTest :
    ConsumerBusinessTransactionManagerPerformanceTest()

@SpringBootTest
@TestPropertySource(properties = ["custom.business-transaction.consumer.persistence=mongodb"])
@ContextConfiguration(classes = [TestApplication::class])
@MongoDbTest
internal class MongodbConsumerBusinessTransactionManagerPerformanceTest :
    ConsumerBusinessTransactionManagerPerformanceTest()

@Suppress("UnnecessaryAbstractClass")
abstract class ConsumerBusinessTransactionManagerPerformanceTest {

  @Autowired private lateinit var cut: ConsumerBusinessTransactionManager

  @Autowired
  private lateinit var businessTransactionRepository: EventOfBusinessTransactionRepositoryPort

  @AfterEach fun cleanUp() = businessTransactionRepository.deleteAll()

  @Disabled // this test is meant to run locally, and only on demand
  @Test
  fun `reading queued events from database returns saved events`() {
    val transactionIdentifier = randomUUID()

    val saveDuration = timed {
      (1..1000).forEach { saveRandomRecord(it.toLong(), "test-processor", transactionIdentifier) }
    }
    println("Saving all events took $saveDuration ms.")

    val readDuration = timed { cut.readEventsFromDatabase(transactionIdentifier, "test-processor") }
    println("Reading all events took $readDuration ms.")
  }

  private fun timed(procedure: () -> Unit): Long {
    val start = System.currentTimeMillis()
    procedure()
    val end = System.currentTimeMillis()
    return end - start
  }

  private fun saveRandomRecord(
      offset: Long = Random().nextLong(),
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
