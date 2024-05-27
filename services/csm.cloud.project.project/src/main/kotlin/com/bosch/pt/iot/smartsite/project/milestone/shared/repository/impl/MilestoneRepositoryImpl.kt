/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId_
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone_
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepositoryExtension
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.converter.MilestoneFilterConverter
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto.MilestoneFilterDto
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Root
import java.util.UUID
import org.springframework.data.domain.Pageable

class MilestoneRepositoryImpl(@PersistenceContext val entityManager: EntityManager) :
    MilestoneRepositoryExtension {

  override fun findMilestoneIdentifiersForFilters(
      filters: MilestoneFilterDto,
      pageable: Pageable
  ): List<MilestoneId> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(UUID::class.java)
    val root = cq.from(Milestone::class.java)
    val predicates = buildPredicates(filters, cb, root)
    val orders = MilestoneFilterConverter.applyOrder(pageable.sort, cb, root)

    cq.select(root.join(Milestone_.identifier).get(MilestoneId_.identifier.name))
        .where(*predicates.toTypedArray())
        .orderBy(orders)

    return entityManager
        .createQuery(cq)
        .setFirstResult(if (pageable.isUnpaged) 0 else pageable.offset.toInt())
        .setMaxResults(if (pageable.isUnpaged) Int.MAX_VALUE else pageable.pageSize)
        .resultList
        .map { it.asMilestoneId() }
  }

  override fun countAllForFilters(filters: MilestoneFilterDto): Long {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(Long::class.java)
    val root = cq.from(Milestone::class.java)
    val predicates = buildPredicates(filters, cb, root)

    cq.select(cb.countDistinct(root)).where(*predicates.toTypedArray())

    return entityManager.createQuery(cq).singleResult
  }

  private fun buildPredicates(
      filters: MilestoneFilterDto,
      cb: CriteriaBuilder,
      root: Root<Milestone>
  ) = MilestoneFilterConverter.convert(filters, cb, root)
}
