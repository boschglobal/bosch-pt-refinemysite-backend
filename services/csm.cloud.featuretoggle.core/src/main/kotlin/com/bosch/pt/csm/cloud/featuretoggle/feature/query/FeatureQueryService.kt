/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.query

import com.bosch.pt.csm.cloud.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.repository.FeatureRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class FeatureQueryService(private val featureRepository: FeatureRepository) {

  @AdminAuthorization
  fun findByFeatureId(featureId: FeatureId): Feature? =
      featureRepository.findByIdentifier(featureId)

  @AdminAuthorization
  fun findByFeatureIdWithDetails(featureId: FeatureId): Feature? =
      featureRepository.findByIdentifierWithDetails(featureId)

  @AdminAuthorization
  fun findAllFeatures(): List<Feature> = featureRepository.findAll(Sort.by("name"))

  @AdminAuthorization
  fun findAllFeaturesWithDetails(): List<Feature> =
      featureRepository.findAllWithDetails(Sort.by("name"))

  @AdminAuthorization
  fun findAllExceptDisabledFeaturesWithDetails(): List<Feature> =
    featureRepository.findAllExceptDisabledWithDetails(Sort.by("name"))
}
