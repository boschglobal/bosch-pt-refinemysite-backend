/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import com.bosch.pt.csm.cloud.usermanagement.craft.event.listener.submitCraft
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
class CraftEventStreamGenerator(
    private val timeLineGenerator: TimeLineGenerator,
    private val eventListener: CraftEventListener,
    private val context: MutableMap<String, SpecificRecordBase>
) {

  fun submitCraft(
      name: String = "craft",
      userName: String = "user",
      eventName: CraftEventEnumAvro = CraftEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((CraftAggregateAvro) -> Unit)? = null
  ): CraftEventStreamGenerator {
    var craft: CraftAggregateAvro? = get(name)

    val defaultAggregateModifications: ((CraftAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
    }

    craft =
        eventListener.submitCraft(
            craft, eventName, defaultAggregateModifications, aggregateModifications)
    context[name] = craft
    return this
  }

  @Suppress("UNCHECKED_CAST") fun <T> get(name: String): T = context[name] as T

  private fun setAuditingInformationAndIncreaseVersion(
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
        user?.apply {
          auditingInformation.setCreatedBy(getAggregateIdentifier(getAggregateIdentifier(this)))
        }
        auditingInformation.setCreatedDate(time.toEpochMilli())
      }
      else -> aggregateIdentifier.increase()
    }
    user?.apply { auditingInformation.setLastModifiedBy(getAggregateIdentifier(this)) }
    auditingInformation.setLastModifiedDate(time.toEpochMilli())
  }

  private fun getAggregateIdentifier(aggregate: SpecificRecordBase) =
      aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro

  private fun AggregateIdentifierAvro.increase() = setVersion(getVersion() + 1)
}
