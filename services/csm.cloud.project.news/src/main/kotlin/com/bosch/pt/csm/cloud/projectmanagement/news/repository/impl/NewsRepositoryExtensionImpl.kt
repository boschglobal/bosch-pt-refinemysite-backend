/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.repository.NewsRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.io.Serializable
import org.springframework.beans.factory.annotation.Value

class NewsRepositoryExtensionImpl : NewsRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun findIdsPartitioned(taskIdentifiers: List<ObjectIdentifier>): List<Long> =
      Lists.partition(taskIdentifiers, partitionSize)
          .map { partition ->
            val cb = entityManager.criteriaBuilder

            val query = cb.createQuery(Serializable::class.java)
            val root = query.from(News::class.java)
            query.where(cb.`in`(root.get<Any>("rootObject")).value(partition))
            query.select(root.get("id"))

            entityManager.createQuery(query).resultList.map { it as Long }
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition ->
      val cb = entityManager.criteriaBuilder
      var delete = cb.createCriteriaDelete(News::class.java)
      val root = delete.from(News::class.java)

      delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
