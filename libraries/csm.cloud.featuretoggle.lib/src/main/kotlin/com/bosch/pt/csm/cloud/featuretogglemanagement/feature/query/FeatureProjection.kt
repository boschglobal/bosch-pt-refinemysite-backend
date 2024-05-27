/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureProjection.Companion.COLLECTION_NAME
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = COLLECTION_NAME)
@TypeAlias(COLLECTION_NAME)
internal data class FeatureProjection(
    @Id val identifier: FeatureIdentifier,
    val name: String,
    val state: FeatureStateEnum,
    // List with subjects (companies, projects, etc.)
    val whiteList: Set<WhitelistedSubject>,
) {
  companion object {
    const val COLLECTION_NAME = "FeatureProjection"
  }
}
