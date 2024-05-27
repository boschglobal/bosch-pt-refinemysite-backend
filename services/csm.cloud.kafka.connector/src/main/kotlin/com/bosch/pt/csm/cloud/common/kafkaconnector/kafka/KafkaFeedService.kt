/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.kafka

import com.bosch.pt.csm.cloud.common.kafka.CustomKafkaHeaders.BUSINESS_TRANSACTION_ID
import com.bosch.pt.csm.cloud.common.kafkaconnector.data.EventDataService
import com.bosch.pt.csm.cloud.common.kafkaconnector.tracing.TraceHeaderTextMapExtract
import datadog.trace.api.Trace
import io.opentracing.Span
import io.opentracing.log.Fields
import io.opentracing.propagation.Format.Builtin.TEXT_MAP_EXTRACT
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import java.util.concurrent.ExecutionException
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.env.Environment
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport

@Service
class KafkaFeedService(
    private val kafkaTemplate: KafkaTemplate<ByteArray, ByteArray>,
    private val environment: Environment,
    private val eventDataService: EventDataService
) {

  @Transactional
  @Trace
  fun feedBatch(table: String): Boolean {
    val tracer = GlobalTracer.get()
    val events = eventDataService.nextBatch(table)

    tracer.activeSpan().setTag("events.count", events.size)
    events
        .map {
            (
                _,
                eventKey,
                data,
                partitionNumber,
                traceHeaderKey,
                traceHeaderValue,
                transactionIdentifier) ->
          val topic = environment.getRequiredProperty("custom.table-mapping.$table")

          // get span of upstream producer from trace header value
          // format: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
          val parentSpan =
              if (traceHeaderKey != null && traceHeaderValue != null) {
                tracer.extract(TEXT_MAP_EXTRACT, TraceHeaderTextMapExtract(traceHeaderValue))
              } else null

          val span =
              tracer
                  .buildSpan("send message to kafka")
                  .withTag("kafka.topic", topic)
                  .asChildOf(parentSpan)
                  .start()

          val transactionIdHeader =
              transactionIdentifier?.let {
                RecordHeader(
                    BUSINESS_TRANSACTION_ID, transactionIdentifier.toString().toByteArray())
              }
          val headers = setOfNotNull(transactionIdHeader)
          val sendFuture =
              tracer.activateSpan(span).use {
                val producedRecord = ProducerRecord(topic, partitionNumber, eventKey, data, headers)
                kafkaTemplate.send(producedRecord)
              }
          return@map Pair(sendFuture, span)
        }
        // IMPORTANT: This step is only executed once the previous map step has been executed for
        // all elements of the collection. This is necessary since the .get() method of the futures
        // should only block after all records have been send to kafka. This is the semantics of the
        // stream compared to a sequence in kotlin. A sequence MUST NOT be used here.
        .forEach { (sendFuture, span) ->
          try {
            sendFuture.get()
          } catch (ex: ExecutionException) {
            markAsFailed(span, ex)
            throw IllegalStateException(ex)
          } catch (ex: InterruptedException) {
            markAsFailed(span, ex)
            Thread.currentThread().interrupt()

            // rollback already sent Kafka events
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()

            // Pretend all events have been processed to not stall a graceful shutdown attempt.
            // Remaining events will still be retried at a later point in time.
            return false
          } finally {
            span.finish()
          }
        }

    if (events.isNotEmpty()) {
      LOGGER.info("Sent ${events.size} events from $table")
      eventDataService.removeBatch(table, events)
    }
    return events.isNotEmpty()
  }

  private fun markAsFailed(span: Span, ex: Exception) {
    span.setTag(Tags.ERROR, true)
    span.log(mapOf(Fields.ERROR_OBJECT to ex))
  }

  companion object {
    private val LOGGER = getLogger(KafkaFeedService::class.java)
  }
}
