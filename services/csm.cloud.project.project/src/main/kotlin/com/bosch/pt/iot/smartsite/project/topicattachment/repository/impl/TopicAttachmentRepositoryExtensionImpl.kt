/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.repository.impl

import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value

class TopicAttachmentRepositoryExtensionImpl : TopicAttachmentRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun getByTopicIdsPartitioned(topicIds: List<Long>) =
      Lists.partition(topicIds, partitionSize)
          .map { partition: List<Long> ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(TopicAttachment::class.java)
            val root = query.from(TopicAttachment::class.java)
            val joinTopic = root.join<TopicAttachment, Topic>("topic")
            query.where(cb.`in`(joinTopic.get<Any>("id")).value(partition))
            entityManager.createQuery(query).resultList
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition: List<Long> ->
      val cb = entityManager.criteriaBuilder
      var delete = cb.createCriteriaDelete(TopicAttachment::class.java)
      val root = delete.from(TopicAttachment::class.java)
      delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
