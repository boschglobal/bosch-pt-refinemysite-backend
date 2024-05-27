/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ObjectRelationRepositoryExtension
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectRelation
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value

class ObjectRelationRepositoryExtensionImpl : ObjectRelationRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition ->
      val cb = entityManager.criteriaBuilder

      var delete = cb.createCriteriaDelete(ObjectRelation::class.java)
      val root = delete.from(ObjectRelation::class.java)
      delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))

      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
