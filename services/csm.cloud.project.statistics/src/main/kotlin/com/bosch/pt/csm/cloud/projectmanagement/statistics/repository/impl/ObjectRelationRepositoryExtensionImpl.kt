/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectRelation
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ObjectRelationRepositoryExtension
import jakarta.persistence.EntityManager

class ObjectRelationRepositoryExtensionImpl(private val entityManager: EntityManager) :
    ObjectRelationRepositoryExtension {

  override fun deleteAll(ids: List<Long>) {

    val cb = entityManager.criteriaBuilder
    var delete = cb.createCriteriaDelete(ObjectRelation::class.java)
    val root = delete.from(ObjectRelation::class.java)
    delete = delete.where(cb.`in`(root.get<Any>("id")).value(ids))

    entityManager.createQuery(delete).executeUpdate()
  }
}
