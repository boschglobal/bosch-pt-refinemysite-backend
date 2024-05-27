/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.repository.impl

import com.bosch.pt.iot.smartsite.common.repository.ArrayQueryUtils.createArrayQuery
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection_
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintSelectionRepositoryExtension
import com.google.common.collect.Lists
import java.sql.PreparedStatement
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate

class TaskConstraintSelectionRepositoryExtensionImpl : TaskConstraintSelectionRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager

  @Autowired private lateinit var jdbcTemplate: JdbcTemplate

  override fun getIdsByTaskIdsPartitioned(taskIds: List<Long>): List<Long> =
      Lists.partition(taskIds, partitionSize)
          .map { partition: List<Long> ->
            entityManager.criteriaBuilder.let { cb ->
              cb.createQuery(Long::class.java).let { query ->
                val root = query.from(TaskConstraintSelection::class.java)
                val joinTask = root.join(TaskConstraintSelection_.task)
                query.where(cb.`in`(joinTask.get<List<Long>>("id")).value(partition))
                // workaround because JPA works with Java classes and kotlin.Long maps to primitive long
                // which would result in error: Specified result type [long] did not match Query selection
                // type [java.io.Serializable]
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                query.select(root.get<java.lang.Long>("id").`as`(Long::class.java))
                entityManager.createQuery(query).resultList
              }
            }
          }
          .flatten()

  override fun deleteConstraintElementsPartitioned(constraintSelectionIds: List<Long>) =
      Lists.partition(constraintSelectionIds, partitionSize).forEach { partition: List<Long> ->
        val query =
            createArrayQuery(
                "DELETE FROM task_action_selection_set WHERE task_action_selection_id IN (%s)",
                partition)
        jdbcTemplate.execute(query) { ps: PreparedStatement ->
          for (i in partition.indices) {
            ps.setString(i + 1, partition[i].toString())
          }
          ps.execute()
        }
      }

  override fun deletePartitioned(ids: List<Long>) =
      Lists.partition(ids, partitionSize).forEach {
        entityManager.criteriaBuilder.let { cb ->
          val delete = cb.createCriteriaDelete(TaskConstraintSelection::class.java)
          val root = delete.from(TaskConstraintSelection::class.java)
          delete.where(cb.`in`(root.get<Any>("id")).value(it))
          entityManager.createQuery(delete).executeUpdate()
        }
      }
}
