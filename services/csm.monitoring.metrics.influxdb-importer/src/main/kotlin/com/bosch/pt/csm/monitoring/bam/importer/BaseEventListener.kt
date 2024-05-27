package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionFinishedMessageKeyAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionStartedMessageKeyAvro
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.reschedule.messages.ProjectRescheduleFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.reschedule.messages.ProjectRescheduleStartedEventAvro
import com.bosch.pt.csm.monitoring.bam.importer.tables.EventsTable
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.jdbc.core.JdbcTemplate

open class BaseEventListener(
    protected val jdbcTemplate: JdbcTemplate,
    private val eventsTable: EventsTable
) {

  protected fun writeEvent(key: SpecificRecordBase, value: SpecificRecordBase, context: String) {
    when (key) {
      is MessageKeyAvro -> eventsTable.ingestAggregateEvent(key, value, context)
      is BusinessTransactionStartedMessageKeyAvro -> writeBusinessTransactionStarted(key, value)
      is BusinessTransactionFinishedMessageKeyAvro -> writeBusinessTransactionFinished(key, value)
      else -> throw IllegalStateException("Unknown message key type")
    }
  }

  protected fun writeBusinessTransactionStarted(
      key: BusinessTransactionStartedMessageKeyAvro,
      value: SpecificRecordBase
  ) {
    jdbcTemplate.update(
        "INSERT INTO businesstransactions VALUES (to_timestamp(?),?,?,?,?) ON CONFLICT DO NOTHING",
        getBusinessTransactionEventDate(value),
        key.transactionIdentifier,
        key.rootContextIdentifier,
        getBusinessTransactionType(value),
        null)
  }

  protected fun writeBusinessTransactionFinished(
      key: BusinessTransactionFinishedMessageKeyAvro,
      value: SpecificRecordBase
  ) {
    jdbcTemplate.update(
        "UPDATE businesstransactions " +
            "SET duration_seconds = EXTRACT(EPOCH FROM to_timestamp(?) - date) " +
            "WHERE transaction_identifier = ?",
        getBusinessTransactionEventDate(value),
        key.transactionIdentifier)
  }

  protected fun getBusinessTransactionEventDate(value: SpecificRecordBase): Long {
    val date =
        when (value) {
          is BatchOperationStartedEventAvro -> value.auditingInformation.date
          is BatchOperationFinishedEventAvro -> value.auditingInformation.date
          is ProjectCopyStartedEventAvro -> value.auditingInformation.date
          is ProjectCopyFinishedEventAvro -> value.auditingInformation.date
          is ProjectImportStartedEventAvro -> value.auditingInformation.date
          is ProjectImportFinishedEventAvro -> value.auditingInformation.date
          is ProjectRescheduleStartedEventAvro -> value.auditingInformation.date
          is ProjectRescheduleFinishedEventAvro -> value.auditingInformation.date
          else -> throw IllegalStateException("Unknown business transaction event")
        }
    return Instant.ofEpochMilli(date).epochSecond
  }

  private fun getBusinessTransactionType(value: SpecificRecordBase): String =
      when (value) {
        is BatchOperationStartedEventAvro -> "BATCH"
        is ProjectCopyStartedEventAvro -> "COPY"
        is ProjectImportStartedEventAvro -> "IMPORT"
        is ProjectRescheduleStartedEventAvro -> "PROJECT_RESCHEDULE"
        else -> throw IllegalStateException("Unknown business transaction event")
      }

  protected fun extractLastModifiedDateAsEpochSeconds(event: SpecificRecordBase) =
      Instant.ofEpochMilli(
              event
                  .extract("aggregate")
                  .extract("auditingInformation")["lastModifiedDate"]
                  .toString()
                  .toLong())
          .epochSecond

  private fun SpecificRecordBase.extract(field: String) = this[field] as SpecificRecordBase

  protected fun ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>.isTombstone() =
      this.value() == null

  protected fun ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>.isAggregateEvent() =
      this.key() is MessageKeyAvro
}
