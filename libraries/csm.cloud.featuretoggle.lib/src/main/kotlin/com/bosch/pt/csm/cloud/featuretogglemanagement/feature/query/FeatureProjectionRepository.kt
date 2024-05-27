/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import org.springframework.data.mongodb.repository.MongoRepository

internal interface FeatureProjectionRepository :
    MongoRepository<FeatureProjection, FeatureIdentifier> {

  fun findByIdentifier(identifier: FeatureIdentifier): FeatureProjection?

  fun findByName(name: String): FeatureProjection?
}
