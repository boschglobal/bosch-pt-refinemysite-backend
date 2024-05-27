/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.AuditedEvent
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatEvent

// marker interface for AvroEventMapper belonging PatLocalEventBus
interface PatAvroEventMapper : AvroEventMapper {
  fun addAggregateIdentifier(event: PatEvent, version: Long): AggregateIdentifierAvro.Builder =
      AggregateIdentifierAvro.newBuilder().apply {
        identifier = event.patId.identifier.toString()
        this.version = version
        type = PAT.name
      }

  fun addAuditingInformation(event: AuditedEvent): EventAuditingInformationAvro.Builder =
      EventAuditingInformationAvro.newBuilder().apply {
        date = event.timestamp.toEpochMilli()
        user = event.userIdentifier.toString()
      }
}
