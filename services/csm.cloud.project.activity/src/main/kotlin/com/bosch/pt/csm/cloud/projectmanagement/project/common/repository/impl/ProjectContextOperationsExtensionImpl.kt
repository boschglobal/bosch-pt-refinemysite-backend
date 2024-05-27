/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query

open class ProjectContextOperationsExtensionImpl<T>
constructor(private val mongoOperations: MongoOperations) : ProjectContextOperationsExtension<T> {

  @Suppress("UNCHECKED_CAST")
  override fun findLatest(identifier: UUID, projectIdentifier: UUID): T? {
    return mongoOperations.findOne(
        query(criteria(identifier, projectIdentifier)).with(Sort.by(desc(ID_VERSION))).limit(1),
        Object::class.java as Class<T>,
        Collections.PROJECT_STATE)
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(identifier: UUID, version: Long, projectIdentifier: UUID): T? {
    return mongoOperations.findOne(
        query(criteria(identifier, version, projectIdentifier)).limit(1),
        Object::class.java as Class<T>,
        Collections.PROJECT_STATE)
  }

  override fun delete(identifier: UUID, projectIdentifier: UUID) {
    mongoOperations.remove(
        query(criteria(identifier, projectIdentifier)), Collections.PROJECT_STATE)
  }

  override fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) {
    mongoOperations.remove(
        query(criteria(identifier, version, projectIdentifier)), Collections.PROJECT_STATE)
  }

  // The projectIdentifier is the shard key
  private fun criteria(identifier: UUID, projectIdentifier: UUID): Criteria =
      Criteria(ID_IDENTIFIER).`is`(identifier).and(PROJECT_IDENTIFIER).`is`(projectIdentifier)

  // The projectIdentifier is the shard key
  private fun criteria(identifier: UUID, version: Long, projectIdentifier: UUID) =
      Criteria(ID_IDENTIFIER)
          .`is`(identifier)
          .and(ID_VERSION)
          .`is`(version)
          .and(PROJECT_IDENTIFIER)
          .`is`(projectIdentifier)
}
