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
import com.bosch.pt.csm.cloud.common.model.AggregateReference
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatCreatedEvent
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class PatCreatedAvroEventMapper : PatAvroEventMapper {

  override fun canMap(event: Any) = event is PatCreatedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey =
      AggregateEventMessageKey(
          AggregateIdentifier(
              type = PAT.value,
              identifier = (event as PatCreatedEvent).patId.identifier,
              version = version,
          ),
          rootContextIdentifier = event.impersonatedUser.identifier,
      )

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase =
      PatCreatedEventAvro.newBuilder()
          .apply {
            description = (event as PatCreatedEvent).description
            hash = event.hash
            issuedAt = event.issuedAt.toEpochMilli()
            expiresAt = event.expiresAt.toEpochMilli()
            impersonatedUser =
                AggregateReference(
                        type = USER.value,
                        identifier = event.impersonatedUser.identifier,
                    )
                    .toAvro()

            scopes = event.scopes.map { PatScopeEnumAvro.valueOf(it.name) }
            type = PatTypeEnumAvro.valueOf(event.type.name)

            aggregateIdentifierBuilder = addAggregateIdentifier(event, version)
            auditingInformationBuilder = addAuditingInformation(event)
          }
          .build()
}
