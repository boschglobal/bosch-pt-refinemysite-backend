/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector

import com.bosch.pt.csm.cloud.common.kafkaconnector.config.CustomConfig
import com.bosch.pt.csm.cloud.common.kafkaconnector.data.EventDataService
import com.bosch.pt.csm.cloud.common.kafkaconnector.kafka.KafkaFeedService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.ResultSet
import java.util.UUID.randomUUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@ActiveProfiles("test")
@SpringBootTest
@SpringJUnitConfig(classes = [KafkaConnectorTest.TestConfig::class])
@TestPropertySource(properties = ["stage=unit-test"])
@MySqlTest
internal class KafkaConnectorTest {

  @Autowired private lateinit var jdbc: JdbcOperations

  @Autowired private lateinit var customConfig: CustomConfig

  @Autowired private lateinit var kafkaTemplate: KafkaTemplate<ByteArray, ByteArray>

  @Autowired private lateinit var eventDataService: EventDataService

  @Autowired private lateinit var kafkaFeedService: KafkaFeedService

  @BeforeEach
  fun setup() {
    tableNames().forEach { tableName: String ->
      jdbc.update("drop table if exists $tableName")
      jdbc.update(
          "create table " +
              tableName +
              " (id bigint(20) not null auto_increment, event longblob," +
              "event_key longblob, partition_number int, trace_header_key varchar(255)," +
              "trace_header_value varchar(255), transaction_identifier varchar(36), primary key(id));")
    }
  }

  @AfterEach
  fun reset() {
    clearMocks(kafkaTemplate)
  }

  @Test
  @Throws(Exception::class)
  fun testFeedBatchKafkaAvailable() {
    val future = mockk<CompletableFuture<SendResult<ByteArray, ByteArray>>>(relaxed = true)
    every { kafkaTemplate.send(any<ProducerRecord<ByteArray, ByteArray>>()) } returns future

    val dataPerTopic = insertTestData()
    val totalEvents = EVENTS_PER_TABLE * dataPerTopic.size

    sendEvents(errorsExpected = false)

    val recordSlots = mutableListOf<ProducerRecord<ByteArray, ByteArray>>()
    verify(exactly = totalEvents, timeout = TEST_TIMEOUT) {
      kafkaTemplate.send(capture(recordSlots))
    }

    recordSlots.forEach { record ->
      val key = String((record.key() as ByteArray))
      assertThat(record.partition()).isEqualTo(partitionOf(key))
      assertThat(dataPerTopic[record.topic()]!!.remove(key)).isTrue()
    }

    verify(exactly = totalEvents, timeout = TEST_TIMEOUT) { future.get() }
    dataPerTopic.values.forEach { unsentRecords -> assertThat(unsentRecords).isEmpty() }
    expectRemainingEventsInDatabase(0)
  }

  @Test
  fun testFeedBatchKafkaOffline() {
    val futures: MutableMap<String, CompletableFuture<SendResult<ByteArray, ByteArray>>> =
        ConcurrentHashMap()

    every { kafkaTemplate.send(any<ProducerRecord<ByteArray, ByteArray>>()) } answers
        {
          futures.computeIfAbsent(
              String((firstArg() as ProducerRecord<ByteArray, ByteArray>).key())) {
                mockFuture()
              }
        }

    val totalEvents = insertTestData().size * EVENTS_PER_TABLE

    sendEvents(errorsExpected = true)

    val recordSlots = mutableListOf<ProducerRecord<ByteArray, ByteArray>>()
    verify(exactly = totalEvents, timeout = TEST_TIMEOUT) {
      kafkaTemplate.send(capture(recordSlots))
    }

    // The sent method should have been called 'totalEvents' times.
    assertThat(recordSlots).hasSize(totalEvents)
    // All data should remain in the database table
    expectRemainingEventsInDatabase(EVENTS_PER_TABLE)
  }

  private fun mockFuture(): CompletableFuture<SendResult<ByteArray, ByteArray>> {
    val future = mockk<CompletableFuture<SendResult<ByteArray, ByteArray>>>()
    try {
      every { future.get() } throws ExecutionException(IllegalStateException("Failed"))
    } catch (e: InterruptedException) {
      throwAsUncheckedException(e)
    } catch (e: ExecutionException) {
      throwAsUncheckedException(e)
    }
    return future
  }

  private fun expectRemainingEventsInDatabase(expected: Int) =
      tableNames()
          .map { tableName ->
            jdbc.query("select count (*) as c from $tableName") { rs: ResultSet, _: Int ->
              rs.getInt("c")
            }
          }
          .flatten()
          .all { count -> count == expected }

  private fun insertTestData(): Map<String, MutableList<String>> =
      tableNames().associate { tableName ->
        customConfig.tableMapping[tableName]!! to generateRandomEvent(tableName)
      }

  private fun generateRandomEvent(tableName: String) =
      (0 until EVENTS_PER_TABLE)
          .map {
            val identifier = randomUUID().toString()
            jdbc.update(
                "insert into $tableName (event_key, event, partition_number) VALUES(?, ?, ?)",
                identifier.toByteArray(), // Add fake event key
                identifier.toByteArray(), // Add fake event data
                partitionOf(identifier))
            return@map identifier
          }
          .toMutableList()

  private fun partitionOf(identifier: String) = abs(identifier.hashCode() % 4)

  private fun tableNames() = customConfig.tableMapping.keys

  private fun sendEvents(errorsExpected: Boolean) {
    while (eventDataService.eventTableNames().any { table ->
      try {
        return@any kafkaFeedService.feedBatch(table)
      } catch (ex: Exception) {
        if (errorsExpected) {
          LOGGER.warn("Sending kafka events failed with reason: $ex")
          return@any false
        } else {
          fail { "Sending kafka events failed with reason: $ex" }
        }
      }
    }) {
      // nothing additional to do
    }
  }

  @TestConfiguration
  class TestConfig {
    @Bean fun kafkaTemplate() = mockk<KafkaTemplate<ByteArray, ByteArray>>(relaxed = true)
  }

  companion object {
    private const val EVENTS_PER_TABLE = 10
    private const val TEST_TIMEOUT = 20000L

    private val LOGGER = getLogger(KafkaConnectorTest::class.java)
  }
}
