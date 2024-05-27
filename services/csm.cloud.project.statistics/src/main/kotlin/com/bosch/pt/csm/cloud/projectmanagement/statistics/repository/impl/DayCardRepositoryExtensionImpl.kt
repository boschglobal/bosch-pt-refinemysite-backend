/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardRepositoryExtension
import jakarta.persistence.EntityManager

class DayCardRepositoryExtensionImpl(private val entityManager: EntityManager) :
    DayCardRepositoryExtension {

  override fun deleteAll(ids: List<Long>) {
    if (ids.isEmpty()) {
      return
    }

    val cb = entityManager.criteriaBuilder
    var delete = cb.createCriteriaDelete(DayCard::class.java)
    val root = delete.from(DayCard::class.java)
    delete = delete.where(cb.`in`(root.get<Any>("id")).value(ids))

    entityManager.createQuery(delete).executeUpdate()
  }
}
