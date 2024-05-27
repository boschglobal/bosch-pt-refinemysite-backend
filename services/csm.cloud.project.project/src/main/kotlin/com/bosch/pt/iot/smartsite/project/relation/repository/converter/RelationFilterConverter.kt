/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository.converter

import com.bosch.pt.iot.smartsite.common.model.AbstractEntity_
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement_
import com.bosch.pt.iot.smartsite.project.relation.model.Relation_
import com.bosch.pt.iot.smartsite.project.relation.repository.converter.RelationSortProperty.ID
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC

object RelationFilterConverter {

  fun convert(filter: RelationFilterDto, cb: CriteriaBuilder, root: Root<Relation>) =
      mutableListOf<Predicate>().apply {
        filterByType(filter, root)
        filterBySourceOrTarget(filter, cb, root)
        filterByProject(filter, cb, root)
      }

  private fun MutableList<Predicate>.filterByProject(
      filter: RelationFilterDto,
      cb: CriteriaBuilder,
      root: Root<Relation>
  ) {
    add(
        cb.equal(
            root
                .join(Relation_.project)
                .join(Project_.identifier)
                .get<UUID>(ProjectId_.identifier.name),
            filter.projectIdentifier.identifier))
  }

  private fun MutableList<Predicate>.filterByType(filter: RelationFilterDto, root: Root<Relation>) {
    if (filter.types.isNotEmpty()) {
      add(root.get(Relation_.type).`in`(filter.types))
    }
  }

  private fun MutableList<Predicate>.filterBySourceOrTarget(
      filter: RelationFilterDto,
      cb: CriteriaBuilder,
      root: Root<Relation>
  ) {
    val predicates = filterBySource(filter, cb, root) + filterByTarget(filter, cb, root)
    if (predicates.isNotEmpty()) {
      add(cb.or(*predicates))
    }
  }

  private fun filterBySource(filter: RelationFilterDto, cb: CriteriaBuilder, root: Root<Relation>) =
      filter.sources
          .groupBy { it.type }
          .map { (type, relationElements) ->
            cb.and(
                cb.equal(root.get(Relation_.source).get(RelationElement_.type), type),
                root
                    .get(Relation_.source)
                    .get(RelationElement_.identifier)
                    .`in`(relationElements.map { it.identifier }))
          }
          .toTypedArray()

  private fun filterByTarget(filter: RelationFilterDto, cb: CriteriaBuilder, root: Root<Relation>) =
      filter.targets
          .groupBy { it.type }
          .map { (type, relationElements) ->
            cb.and(
                cb.equal(root.get(Relation_.target).get(RelationElement_.type), type),
                root
                    .get(Relation_.target)
                    .get(RelationElement_.identifier)
                    .`in`(relationElements.map { it.identifier }))
          }
          .toTypedArray()

  fun applyOrder(sort: Sort, cb: CriteriaBuilder, root: Root<Relation>): List<Order> =
      sort
          .map {
            when (it.property) {
              ID -> buildOrderExpression(root.get(AbstractEntity_.identifier), ASC, cb)
              else -> throw IllegalArgumentException("Unknown property: ${it.property}")
            }
          }
          .toList()

  private fun buildOrderExpression(
      expression: Expression<*>,
      direction: Sort.Direction,
      cb: CriteriaBuilder,
  ) = if (direction.isAscending) cb.asc(expression) else cb.desc(expression)
}
