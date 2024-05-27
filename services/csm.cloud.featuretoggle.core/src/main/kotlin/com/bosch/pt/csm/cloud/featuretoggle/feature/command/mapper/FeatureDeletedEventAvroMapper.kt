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
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDeletedEventAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class FeatureDeletedEventAvroMapper : FeaturetoggleAvroEventMapper {
  override fun canMap(event: Any): Boolean = event is FeatureDeletedEvent

  override fun mapToKey(event: Any, version: Long): AggregateEventMessageKey {
    require(event is FeatureDeletedEvent)

    return AggregateEventMessageKey(
        AggregateIdentifier(FEATURE_TOGGLE.name, event.featureId.identifier, version),
        event.featureId.identifier)
  }

  override fun mapToValue(event: Any, version: Long): SpecificRecordBase {
    require(event is FeatureDeletedEvent)

    return FeatureDeletedEventAvro.newBuilder()
        .apply {
          featureName = event.name
          aggregateIdentifierBuilder = addAggregateIdentifier(event, version)
          auditingInformationBuilder = addAuditingInformation(event)
        }
        .build()
  }
}
