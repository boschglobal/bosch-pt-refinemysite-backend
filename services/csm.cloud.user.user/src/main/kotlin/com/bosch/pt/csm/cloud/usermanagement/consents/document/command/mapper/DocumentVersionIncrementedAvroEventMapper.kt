/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.consents.common.ConsentsAggregateTypeEnum.DOCUMENT
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentVersionIncrementedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentVersionAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentVersionIncrementedEventAvro
import org.apache.avro.specific.SpecificRecordBase

class DocumentVersionIncrementedAvroEventMapper : AvroEventMapper {
  override fun canMap(event: Any) = event is DocumentVersionIncrementedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey {
    require(event is DocumentVersionIncrementedEvent)

    return AggregateEventMessageKey(
        AggregateIdentifier(DOCUMENT.value, event.documentIdentifier.identifier, version),
        event.documentIdentifier.identifier)
  }

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase {
    require(event is DocumentVersionIncrementedEvent)

    return DocumentVersionIncrementedEventAvro.newBuilder()
        .apply {
          versionBuilder =
              DocumentVersionAvro.newBuilder().apply {
                identifier = event.version.identifier.toString()
                lastChanged = event.version.lastChanged.toEpochMilli()
              }
          aggregateIdentifierBuilder =
              AggregateIdentifierAvro.newBuilder().apply {
                identifier = event.documentIdentifier.toString()
                this.version = version
                type = DOCUMENT.value
              }
          auditingInformationBuilder =
              EventAuditingInformationAvro.newBuilder().apply {
                date = event.timestamp.toEpochMilli()
                user = event.userIdentifier.toString()
              }
        }
        .build()
  }
}
