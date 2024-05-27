/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId_
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task_
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepositoryExtension
import com.bosch.pt.iot.smartsite.project.task.shared.repository.converter.TaskFilterConverter
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import com.google.common.collect.Lists.partition
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.io.Serializable
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable

class TaskRepositoryImpl : TaskRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun findTaskIdentifiersForFilters(
      filters: TaskFilterDto,
      pageable: Pageable
  ): List<TaskId> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(UUID::class.java)

    val root = cq.from(Task::class.java)
    val predicates = buildPredicates(filters, cb, root)
    val orders = TaskFilterConverter.applyOrder(pageable.sort, cb, root)

    val taskIdJoin = root.join(Task_.identifier).get<UUID>(TaskId_.identifier.name)
    cq.select(taskIdJoin).where(*predicates.toTypedArray()).orderBy(orders).groupBy(taskIdJoin)

    return entityManager
        .createQuery(cq)
        .setFirstResult(pageable.offset.toInt())
        .setMaxResults(pageable.pageSize)
        .resultList
        .map { it.asTaskId() }
  }

  override fun findTaskIdsForFilters(filters: TaskFilterDto, pageable: Pageable): List<Long> {
    val cb = entityManager.criteriaBuilder

    // should be Long actually, but the AbstractPersistable_ metamodel mandates Serializable instead
    val cq = cb.createQuery(Serializable::class.java)

    val root = cq.from(Task::class.java)
    val predicates = buildPredicates(filters, cb, root)
    val orders = TaskFilterConverter.applyOrder(pageable.sort, cb, root)

    cq.select(root.get(Task_.id))
        .where(*predicates.toTypedArray())
        .orderBy(orders)
        .groupBy(root.get(Task_.id))

    return entityManager
        .createQuery(cq)
        .setFirstResult(pageable.offset.toInt())
        .setMaxResults(pageable.pageSize)
        .resultList
        .map { it as Long }
  }

  override fun countAllForFilters(taskFilters: TaskFilterDto): Long {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(Long::class.java)

    val root = cq.from(Task::class.java)
    val predicates = buildPredicates(taskFilters, cb, root)
    cq.select(cb.countDistinct(root)).where(*predicates.toTypedArray())

    return entityManager.createQuery(cq).singleResult
  }

  /** Builds the predicates to be used on the where clause of the query. */
  private fun buildPredicates(
      filters: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>
  ): List<Predicate> = TaskFilterConverter.converterFilter(filters, cb, root)

  override fun deletePartitioned(ids: List<Long>) {
    partition(ids, partitionSize).forEach { partition: List<Long> ->
      val cb = entityManager.criteriaBuilder
      var delete = cb.createCriteriaDelete(Task::class.java)
      val root = delete.from(Task::class.java)
      delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
      entityManager.createQuery(delete).executeUpdate()
    }
  }

  override fun markAsDeleted(taskId: Long) {
    entityManager
        .createNativeQuery("update task set deleted = true where id = :id")
        .apply { setParameter("id", taskId) }
        .executeUpdate()
  }

  override fun markAsDeleted(identifiers: List<Long>) {
    entityManager
        .createNativeQuery("update task set deleted = true where id in (:identifiers)")
        .apply { setParameter("identifiers", identifiers) }
        .executeUpdate()
  }
}
