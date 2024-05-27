/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message_
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value

class MessageRepositoryExtensionImpl : MessageRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private val entityManager: EntityManager? = null

  override fun getIdsByTopicIdsPartitioned(topicIds: List<Long>) =
      Lists.partition(topicIds, partitionSize)
          .map { partition: List<Long> ->
            val cb = entityManager!!.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(Message::class.java)
            val joinTopic = root.join(Message_.topic)
            query.where(cb.`in`(joinTopic.get<Any>("id")).value(partition))
            // workaround because JPA works with Java classes and kotlin.Long maps to primitive long
            // which results in error: Specified result type [long] did not match Query selection
            // type [java.io.Serializable]
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            query.select(root.get<java.lang.Long>("id").`as`(Long::class.java))
            entityManager.createQuery(query).resultList
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition: List<Long> ->
      val cb = entityManager!!.criteriaBuilder
      val delete = cb.createCriteriaDelete(Message::class.java)
      val root = delete.from(Message::class.java)
      delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
