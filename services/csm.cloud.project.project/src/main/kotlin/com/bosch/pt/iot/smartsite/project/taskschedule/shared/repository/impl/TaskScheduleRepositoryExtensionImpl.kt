/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.impl

import com.bosch.pt.iot.smartsite.common.repository.ArrayQueryUtils.createArrayQuery
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId_
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleSlot
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleSlot_
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule_
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepositoryExtension
import com.google.common.collect.Lists
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import java.sql.PreparedStatement
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate

class TaskScheduleRepositoryExtensionImpl : TaskScheduleRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  @Autowired private lateinit var jdbcTemplate: JdbcTemplate

  @Suppress("UNCHECKED_CAST")
  override fun findOneByIdentifier(identifier: TaskScheduleId): TaskSchedule? {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(TaskSchedule::class.java)

    val root = cq.from(TaskSchedule::class.java)
    val slot =
        root.fetch<TaskSchedule, TaskScheduleSlot>(TaskSchedule_.SLOTS, JoinType.LEFT)
            as Join<TaskSchedule, TaskScheduleSlot>
    slot.fetch<TaskScheduleSlot, DayCard>(TaskScheduleSlot_.DAY_CARD, JoinType.LEFT)

    cq.where(
        cb.equal(
            root.join(TaskSchedule_.identifier).get<UUID>(TaskScheduleId_.identifier.name),
            identifier.identifier))

    return entityManager.createQuery(cq).resultList.firstOrNull()
  }

  override fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long> =
      Lists.partition(taskIds, partitionSize)
          .map { partition: List<Long> ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(TaskSchedule::class.java)
            val joinTask = root.join(TaskSchedule_.task)

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

  override fun deleteScheduleSlotsPartitioned(taskScheduleIds: List<Long>) =
      Lists.partition(taskScheduleIds, partitionSize).forEach { partition: List<Long> ->
        val query =
            createArrayQuery(
                "DELETE FROM taskschedule_taskscheduleslot WHERE taskschedule_id IN (%s)",
                partition)

        jdbcTemplate.execute(query) { ps: PreparedStatement ->
          for (i in partition.indices) {
            ps.setString(i + 1, partition[i].toString())
          }
          ps.execute()
        }
      }

  override fun deletePartitioned(ids: List<Long>) =
      Lists.partition(ids, partitionSize).forEach { partition: List<Long> ->
        val cb = entityManager.criteriaBuilder
        var delete = cb.createCriteriaDelete(TaskSchedule::class.java)
        val root = delete.from(TaskSchedule::class.java)

        delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
        entityManager.createQuery(delete).executeUpdate()
      }
}
