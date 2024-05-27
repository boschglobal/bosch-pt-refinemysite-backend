/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.consents.common.ConsentsAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.UserConsentedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.UserConsentedEventAvro
import org.apache.avro.specific.SpecificRecordBase

class UserConsentedAvroEventMapper : AvroEventMapper {
  override fun canMap(event: Any) = event is UserConsentedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey {
    require(event is UserConsentedEvent)

    return AggregateEventMessageKey(
        AggregateIdentifier(USER.value, event.userIdentifier.toUuid(), version),
        event.userIdentifier.toUuid())
  }

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase {
    require(event is UserConsentedEvent)

    return UserConsentedEventAvro.newBuilder()
        .apply {
          documentVersionIdentifier = event.documentVersionIdentifier.toString()
          consentedAt = event.timestamp.toEpochMilli()
          aggregateIdentifierBuilder =
              AggregateIdentifierAvro.newBuilder().apply {
                identifier = event.userIdentifier.toString()
                this.version = version
                type = USER.value
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
