/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.AuditedEvent
import com.bosch.pt.csm.cloud.featuretoggle.common.command.api.FeaturetoggleContextEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE

// marker interface for AvroEventMapper belonging to FeaturetoggleContextLocalEventBus
interface FeaturetoggleAvroEventMapper : AvroEventMapper {

  fun addAggregateIdentifier(event: FeaturetoggleContextEvent, version: Long): AggregateIdentifierAvro.Builder =
      AggregateIdentifierAvro.newBuilder().apply {
        identifier = event.featureId.identifier.toString()
        this.version = version
        type = FEATURE_TOGGLE.name
      }

  fun addAuditingInformation(event: AuditedEvent): EventAuditingInformationAvro.Builder =
      EventAuditingInformationAvro.newBuilder().apply {
        date = event.timestamp.toEpochMilli()
        user = event.userIdentifier.toString()
      }
}
