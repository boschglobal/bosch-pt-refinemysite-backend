/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.DayCardRepositoryExtension
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query

open class DayCardRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    DayCardRepositoryExtension {

  override fun find(identifier: UUID, version: Long, projectIdentifier: UUID): DayCard =
      mongoOperations.findOne(
          findDayCardWithVersionQuery(identifier, version, projectIdentifier),
          DayCard::class.java,
          Collections.PROJECT_STATE)!!

  override fun findDayCards(projectIdentifier: UUID): List<DayCard> =
      mongoOperations.find(
          query(
                  CriteriaOperator.and(
                      ProjectContextCriteriaSnippets.belongsToProject(projectIdentifier),
                      isDayCard()))
              .with(Sort.by(Sort.Direction.DESC, "identifier", "version")),
          Collections.PROJECT_STATE)

  override fun deleteDayCard(identifier: UUID, projectIdentifier: UUID) {
    val criteria =
        CriteriaOperator.and(belongsToProject(projectIdentifier), isAnyVersionOfDayCard(identifier))
    mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
  }

  private fun belongsToProject(identifier: UUID): Criteria =
      Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)

  private fun isAnyVersionOfDayCard(identifier: UUID): Criteria =
      Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_DAYCARD).and(ID_IDENTIFIER).`is`(identifier)

  private fun isDayCard(): Criteria = Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_DAYCARD)

  private fun findDayCardWithVersionQuery(
      identifier: UUID,
      version: Long,
      projectIdentifier: UUID
  ): Query =
      query(
              Criteria.where(ID_TYPE)
                  .`is`(ID_TYPE_VALUE_DAYCARD)
                  .and(ID_IDENTIFIER)
                  .`is`(identifier)
                  .and(CommonAttributeNames.ID_VERSION)
                  .`is`(version)
                  // we provide the shard key here to improve performance
                  .and(PROJECT_IDENTIFIER)
                  .`is`(projectIdentifier))
          .limit(1)
}
