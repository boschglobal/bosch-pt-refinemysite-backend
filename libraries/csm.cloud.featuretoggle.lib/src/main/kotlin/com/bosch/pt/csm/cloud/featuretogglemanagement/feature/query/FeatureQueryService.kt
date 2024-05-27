/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.ENABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FeatureQueryService {

  @Autowired private lateinit var featureProjectionRepository: FeatureProjectionRepository

  fun isFeatureEnabled(featureName: String, subject: WhitelistedSubject) =
      featureProjectionRepository.findByName(featureName)?.let {
        it.state == ENABLED || it.state == WHITELIST_ACTIVATED && it.whiteList.contains(subject)
      }
          ?: false

  fun getEnabledFeatures(subject: WhitelistedSubject): List<String> {
    return featureProjectionRepository
        .findAll()
        .filter { feature ->
          feature.state == ENABLED ||
              feature.state == WHITELIST_ACTIVATED && feature.whiteList.contains(subject)
        }
        .map { it.name }
  }
}
