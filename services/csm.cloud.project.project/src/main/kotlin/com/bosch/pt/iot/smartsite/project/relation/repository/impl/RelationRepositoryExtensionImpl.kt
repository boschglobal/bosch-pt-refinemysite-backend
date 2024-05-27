/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository.impl

import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.Relation_
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepositoryExtension
import com.bosch.pt.iot.smartsite.project.relation.repository.converter.RelationFilterConverter
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import java.util.UUID
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Pageable

class RelationRepositoryExtensionImpl(
    private val entityManager: EntityManager,
) : RelationRepositoryExtension {

  override fun findForFilters(filters: RelationFilterDto, pageable: Pageable): List<UUID> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(UUID::class.java).distinct(true)
    val root = cq.from(Relation::class.java)
    val predicates = buildPredicates(filters, cb, root)
    val orders = RelationFilterConverter.applyOrder(pageable.sort, cb, root)

    cq.select(root.get(Relation_.identifier)).where(*predicates.toTypedArray()).orderBy(orders)

    return entityManager
        .createQuery(cq)
        .setFirstResult(if (pageable.isUnpaged) 0 else pageable.offset.toInt())
        .setMaxResults(if (pageable.isUnpaged) Integer.MAX_VALUE else pageable.pageSize)
        .resultList
  }

  override fun countForFilters(filters: RelationFilterDto): Long {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(Long::class.java).distinct(true)
    val root = cq.from(Relation::class.java)
    val predicates = buildPredicates(filters, cb, root)

    cq.select(cb.countDistinct(root)).where(*predicates.toTypedArray())

    return entityManager.createQuery(cq).singleResult
  }

  private fun buildPredicates(
      filters: RelationFilterDto,
      cb: CriteriaBuilder,
      root: Root<Relation>
  ) = RelationFilterConverter.convert(filters, cb, root)
}
