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
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentChangedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentChangedEventAvro
import org.apache.avro.specific.SpecificRecordBase

class DocumentChangedAvroEventMapper : AvroEventMapper {
  override fun canMap(event: Any) = event is DocumentChangedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey {
    require(event is DocumentChangedEvent)

    return AggregateEventMessageKey(
        AggregateIdentifier(DOCUMENT.value, event.documentIdentifier.identifier, version),
        event.documentIdentifier.identifier)
  }

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase {
    require(event is DocumentChangedEvent)

    return DocumentChangedEventAvro.newBuilder()
        .apply {
          title = event.title
          url = event.url?.toString()
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
