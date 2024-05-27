/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.shared.repository

import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FeatureRepository : JpaRepository<Feature, Long> {

  fun findByIdentifier(identifier: FeatureId): Feature?

  @EntityGraph(attributePaths = ["whitelistedSubjects"])
  @Query("select f from Feature f where f.identifier = :identifier")
  fun findByIdentifierWithDetails(identifier: FeatureId): Feature?

  fun findByName(name: String): Feature?

  @EntityGraph(attributePaths = ["whitelistedSubjects"])
  @Query("select f from Feature f where f.name = :name")
  fun findByNameWithDetails(name: String): Feature?

  @EntityGraph(attributePaths = ["whitelistedSubjects"])
  @Query("select f from Feature f")
  fun findAllWithDetails(sort: Sort): List<Feature>

  @EntityGraph(attributePaths = ["whitelistedSubjects"])
  @Query(
      "select f from Feature f " +
          "where f.state = com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED")
  fun findAllExceptDisabledWithDetails(sort: Sort): List<Feature>

  fun existsByName(name: String): Boolean
}
