/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
open class AbstractEventStreamGenerator(
    private val context: MutableMap<String, SpecificRecordBase>
) {

  @Suppress("UNCHECKED_CAST") protected fun <T> get(name: String): T = context[name] as T

  protected fun setAuditingInformationAndIncreaseVersion(
      aggregate: SpecificRecordBase,
      event: String,
      userName: String,
      time: Instant
  ) {
    val auditingInformation = (aggregate.get("auditingInformation") as AuditingInformationAvro)
    val aggregateIdentifier = (aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro)
    val user = get<SpecificRecordBase?>(userName)
    when (event) {
      "CREATED" -> {
        user?.apply { auditingInformation.setCreatedBy(getAggregateIdentifier(this)) }
        auditingInformation.setCreatedDate(time.toEpochMilli())
      }
      else -> aggregateIdentifier.increase()
    }
    user?.apply { auditingInformation.setLastModifiedBy(getAggregateIdentifier(this)) }
    auditingInformation.setLastModifiedDate(time.toEpochMilli())
  }

  protected fun getAggregateIdentifier(aggregate: SpecificRecordBase) =
      aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro

  private fun AggregateIdentifierAvro.increase() = setVersion(getVersion() + 1)
}
