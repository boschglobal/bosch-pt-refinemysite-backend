/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.mapper

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleAvroEventMapper
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class FeatureCreatedAvroEventMapper : FeaturetoggleAvroEventMapper {
  override fun canMap(event: Any) = event is FeatureCreatedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey {
    require(event is FeatureCreatedEvent)

    return AggregateEventMessageKey(
        AggregateIdentifier(FEATURE_TOGGLE.name, event.featureId.identifier, version),
        event.featureId.identifier)
  }

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase {
    require(event is FeatureCreatedEvent)
    return FeatureCreatedEventAvro.newBuilder()
        .apply {
          featureName = event.name
          aggregateIdentifierBuilder = addAggregateIdentifier(event, version)
          auditingInformationBuilder = addAuditingInformation(event)
        }
        .build()
  }
}
