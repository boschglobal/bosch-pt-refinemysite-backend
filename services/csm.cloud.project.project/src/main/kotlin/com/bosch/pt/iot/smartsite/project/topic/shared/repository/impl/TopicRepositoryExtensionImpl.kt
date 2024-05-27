/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic_
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.Assert

class TopicRepositoryExtensionImpl : TopicRepositoryExtension {
  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long> =
      Lists.partition(taskIds, partitionSize)
          .map { partition: List<Long> ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(Topic::class.java)
            val joinTask = root.join(Topic_.task)
            query.where(cb.`in`(joinTask.get<Any>("id")).value(partition))
            // workaround because JPA works with Java classes and kotlin.Long maps to primitive long
            // which would result in error: Specified result type [long] did not match Query
            // selection
            // type [java.io.Serializable]
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            query.select(root.get<java.lang.Long>("id").`as`(Long::class.java))
            entityManager.createQuery(query).resultList
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition: List<Long> ->
      val cb = entityManager.criteriaBuilder
      val delete = cb.createCriteriaDelete(Topic::class.java)
      val root = delete.from(Topic::class.java)
      delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }

  override fun markAsDeleted(topicId: Long) {
    Assert.notNull(topicId, "Topic id must not be null")
    val query = entityManager.createNativeQuery("update topic set deleted = true where id = :id")
    query.setParameter("id", topicId)
    query.executeUpdate()
  }
}
