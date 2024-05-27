/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.repository.impl

import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value

class TaskAttachmentRepositoryExtensionImpl : TaskAttachmentRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun getByTaskIdsPartitioned(taskIds: List<Long>): List<TaskAttachment> =
      Lists.partition(taskIds, partitionSize)
          .map { partition: List<Long> ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(TaskAttachment::class.java)
            val root = query.from(TaskAttachment::class.java)
            val joinTask = root.join<TaskAttachment, Task>("task")
            query.where(cb.`in`(joinTask.get<Any>("id")).value(partition))
            entityManager.createQuery(query).resultList
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize).forEach { partition: List<Long> ->
      val cb = entityManager.criteriaBuilder
      var delete = cb.createCriteriaDelete(TaskAttachment::class.java)
      val root = delete.from(TaskAttachment::class.java)
      delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }
}
