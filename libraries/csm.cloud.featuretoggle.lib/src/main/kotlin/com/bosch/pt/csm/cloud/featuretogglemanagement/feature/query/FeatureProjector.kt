/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.DISABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.ENABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.UpstreamFeatureEvent
import org.springframework.stereotype.Component

@Component
internal class FeatureProjector(
    private val featureProjectionRepository: FeatureProjectionRepository,
) {

  private val initialFeatureState = WHITELIST_ACTIVATED

  fun handle(event: UpstreamFeatureEvent) {
    with(featureProjectionRepository) {
      when (event) {
        is FeatureCreatedEvent ->
            save(
                FeatureProjection(
                    event.featureIdentifier, event.name, initialFeatureState, emptySet()))
        is FeatureDeletedEvent -> deleteById(event.featureIdentifier)
        is FeatureEnabledEvent ->
            findByIdentifier(event.featureIdentifier)?.let { save(it.copy(state = ENABLED)) }
        is FeatureDisabledEvent ->
            findByIdentifier(event.featureIdentifier)?.let { save(it.copy(state = DISABLED)) }
        is FeatureWhitelistActivatedEvent ->
            findByIdentifier(event.featureIdentifier)?.let {
              save(it.copy(state = WHITELIST_ACTIVATED))
            }
        is SubjectAddedToWhitelistEvent ->
            findByIdentifier(event.featureIdentifier)?.let {
              save(it.copy(whiteList = it.whiteList + event.subject))
            }
        is SubjectDeletedFromWhitelistEvent ->
            findByIdentifier(event.featureIdentifier)?.let {
              save(it.copy(whiteList = it.whiteList - event.subject))
            }
      }
    }
  }
}
