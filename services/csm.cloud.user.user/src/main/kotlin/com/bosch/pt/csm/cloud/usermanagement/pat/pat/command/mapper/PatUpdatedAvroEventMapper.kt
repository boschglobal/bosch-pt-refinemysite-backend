/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.mapper

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatUpdatedEvent
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class PatUpdatedAvroEventMapper : PatAvroEventMapper {

  override fun canMap(event: Any) = event is PatUpdatedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey =
      AggregateEventMessageKey(
          AggregateIdentifier(
              type = PAT.value,
              identifier = (event as PatUpdatedEvent).patId.identifier,
              version = version,
          ),
          rootContextIdentifier = event.impersonatedUser.identifier,
      )

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase =
      PatUpdatedEventAvro.newBuilder()
          .apply {
            description = (event as PatUpdatedEvent).description
            expiresAt = event.expiresAt.toEpochMilli()
            scopes = event.scopes.map { PatScopeEnumAvro.valueOf(it.name) }

            aggregateIdentifierBuilder = addAggregateIdentifier(event, version)
            auditingInformationBuilder = addAuditingInformation(event)
          }
          .build()
}
