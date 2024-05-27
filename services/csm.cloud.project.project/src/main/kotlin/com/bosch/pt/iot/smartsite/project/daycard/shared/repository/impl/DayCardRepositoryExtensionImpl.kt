/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard_
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepositoryExtension
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.google.common.collect.Lists
import java.util.function.Consumer
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Root
import org.springframework.beans.factory.annotation.Value

class DayCardRepositoryExtensionImpl : DayCardRepositoryExtension {

  @Value("\${db.in.max-size}") private val partitionSize = 0

  @PersistenceContext private lateinit var entityManager: EntityManager
  override fun getIdsByTaskScheduleIdsPartitioned(taskScheduleIds: List<Long>): List<Long> =
      Lists.partition(taskScheduleIds, partitionSize)
          .map { partition: List<Long> ->
            val cb: CriteriaBuilder = entityManager.criteriaBuilder
            cb.createQuery(Long::class.java).let { query: CriteriaQuery<Long> ->
              val root = query.from(DayCard::class.java)
              val joinTaskSchedule: Join<DayCard, TaskSchedule> = root.join(DayCard_.taskSchedule)
              query.where(cb.`in`(joinTaskSchedule.get<Any>("id")).value(partition))
              // workaround because JPA works with Java classes and kotlin.Long maps to primitive long
              // which would result in error: Specified result type [long] did not match Query selection
              // type [java.io.Serializable]
              @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
              query.select(root.get<java.lang.Long>("id").`as`(Long::class.java))
              entityManager.createQuery(query).resultList
            }
          }
          .flatten()

  override fun deletePartitioned(ids: List<Long>) {
    Lists.partition(ids, partitionSize)
        .forEach(
            Consumer { partition: List<Long> ->
              val cb: CriteriaBuilder = entityManager.criteriaBuilder
              var delete: CriteriaDelete<DayCard> = cb.createCriteriaDelete(DayCard::class.java)
              val root: Root<DayCard> = delete.from(DayCard::class.java)
              delete = delete.where(cb.`in`(root.get<Any>("id")).value(partition))
              entityManager.createQuery(delete).executeUpdate()
            })
  }
}
