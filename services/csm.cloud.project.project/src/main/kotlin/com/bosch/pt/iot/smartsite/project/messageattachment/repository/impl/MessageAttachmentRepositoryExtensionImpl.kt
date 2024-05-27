/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.repository.impl

import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value

class MessageAttachmentRepositoryExtensionImpl : MessageAttachmentRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun getByMessageIdsPartitioned(messageIds: List<Long>) =
      Lists.partition(messageIds, partitionSize)
          .map { partition: List<Long?> ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(MessageAttachment::class.java)
            val root = query.from(MessageAttachment::class.java)
            val joinMessage = root.join<MessageAttachment, Message>("message")
            query.where(cb.`in`(joinMessage.get<Any>("id")).value(partition))
            entityManager.createQuery(query).resultList
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition: List<Long?> ->
      val cb = entityManager.criteriaBuilder
      val delete = cb.createCriteriaDelete(MessageAttachment::class.java)
      val root = delete.from(MessageAttachment::class.java)
      delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
