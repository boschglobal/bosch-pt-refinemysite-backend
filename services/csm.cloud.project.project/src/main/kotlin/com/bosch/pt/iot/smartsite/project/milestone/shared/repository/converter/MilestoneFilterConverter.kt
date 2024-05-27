/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository.converter

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable_
import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler.join
import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler.joinLeft
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId_
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList_
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone_
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto.MilestoneFilterDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId_
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft_
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId_
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC

object MilestoneFilterConverter {

  fun convert(filter: MilestoneFilterDto, cb: CriteriaBuilder, root: Root<Milestone>) =
      mutableListOf<Predicate>().apply {
        filterByProject(filter, root, cb)
        filterByTypes(filter, root, cb)
        filterByWorkAreasOrHeader(filter, root, cb)
        filterByStartAndEndDate(filter, root, cb)
        filterByMilestoneLists(filter, root)
      }

  private fun MutableList<Predicate>.filterByProject(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ) {
    val milestoneProjectJoin = join(root, Milestone_.project).get()
    add(
        cb.equal(
            milestoneProjectJoin.join(Project_.identifier).get<UUID>(ProjectId_.identifier.name),
            filter.projectIdentifier?.identifier))
  }

  private fun MutableList<Predicate>.filterByTypes(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ) {
    val predicates =
        setOf(
                buildFilterByProjectAndInvestorTypePredicate(filter, root, cb),
                buildFilterByCraftTypePredicate(filter, root, cb))
            .filterNotNull()
            .toTypedArray()

    if (predicates.size == 1) {
      add(predicates.single())
    } else if (predicates.size > 1) {
      add(cb.or(*predicates))
    }
  }

  private fun buildFilterByProjectAndInvestorTypePredicate(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ): Predicate? {
    return if (filter.types.isNotEmpty()) {
      val allTypes = root.get(Milestone_.type).`in`(filter.types)
      val excludeTypeCraft = cb.notEqual(root.get(Milestone_.type), CRAFT)
      cb.and(allTypes, excludeTypeCraft)
    } else {
      null
    }
  }

  private fun buildFilterByCraftTypePredicate(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ): Predicate? {
    return if (filter.craftIdentifiers.isNotEmpty()) {
      val milestoneCraftJoin = joinLeft(root, Milestone_.craft).get()
      cb.and(
          cb.equal(root.get(Milestone_.type), CRAFT),
          milestoneCraftJoin
              .join(ProjectCraft_.identifier)
              .get<UUID>(ProjectCraftId_.identifier.name)
              .`in`(filter.craftIdentifiers.map { it.identifier }))
    } else if (filter.types.contains(CRAFT)) {
      cb.equal(root.get(Milestone_.type), CRAFT)
    } else {
      null
    }
  }

  private fun MutableList<Predicate>.filterByWorkAreasOrHeader(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ) {
    val predicates =
        setOf(
                buildFilterByWorkAreasPredicate(filter, root),
                buildFilterByEmptyWorkAreaPredicate(filter, root, cb),
                buildFilterByHeaderPredicate(filter, cb, root))
            .filterNotNull()
            .toTypedArray()

    if (predicates.size == 1) {
      add(predicates.single())
    } else if (predicates.size > 1) {
      add(cb.or(*predicates))
    }
  }

  private fun buildFilterByWorkAreasPredicate(
      filter: MilestoneFilterDto,
      root: Root<Milestone>
  ): Predicate? {
    val workAreaIds = filter.workAreaIdentifiers.mapNotNull(WorkAreaIdOrEmpty::identifier)
    if (workAreaIds.isEmpty()) {
      return null
    }
    return joinLeft(root, Milestone_.workArea)
        .get()
        .join(WorkArea_.identifier)
        .get<UUID>(WorkAreaId_.identifier.name)
        .`in`(workAreaIds.map { it.identifier })
  }

  private fun buildFilterByEmptyWorkAreaPredicate(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ): Predicate? {
    val includeEmpty = filter.workAreaIdentifiers.any(WorkAreaIdOrEmpty::isEmpty)
    if (!includeEmpty) {
      return null
    }
    // exclude header milestones because otherwise filtering for milestones in the "without working
    // area" section would also return the header milestones that do not have a working area too.
    return cb.and(root.get(Milestone_.workArea).isNull, cb.isFalse(root.get(Milestone_.header)))
  }

  private fun buildFilterByHeaderPredicate(
      filter: MilestoneFilterDto,
      cb: CriteriaBuilder,
      root: Root<Milestone>
  ): Predicate? {
    val includeHeader = filter.header
    if (includeHeader == null || includeHeader == false) {
      return null
    }
    return cb.isTrue(root.get(Milestone_.header))
  }

  private fun MutableList<Predicate>.filterByStartAndEndDate(
      filter: MilestoneFilterDto,
      root: Root<Milestone>,
      cb: CriteriaBuilder
  ) {
    val rangeStartDate = filter.rangeStartDate
    val rangeEndDate = filter.rangeEndDate

    if (rangeStartDate != null && rangeEndDate != null) {
      add(cb.between(root.get(Milestone_.date), rangeStartDate, rangeEndDate))
    } else if (rangeStartDate != null) {
      add(cb.greaterThanOrEqualTo(root.get(Milestone_.date), rangeStartDate))
    } else if (rangeEndDate != null) {
      add(cb.lessThanOrEqualTo(root.get(Milestone_.date), rangeEndDate))
    }
  }

  private fun MutableList<Predicate>.filterByMilestoneLists(
      filter: MilestoneFilterDto,
      root: Root<Milestone>
  ) {
    if (filter.milestoneListIdentifiers.isNotEmpty()) {
      val milestoneMilestoneListJoin = join(root, Milestone_.milestoneList).get()
      add(
          milestoneMilestoneListJoin
              .join(MilestoneList_.identifier)
              .get<UUID>(MilestoneListId_.identifier.name)
              .`in`(filter.milestoneListIdentifiers.map { it.identifier }))
    }
  }

  fun applyOrder(sort: Sort?, cb: CriteriaBuilder, root: Root<Milestone>): List<Order> {
    if (sort == null) {
      return listOf()
    }

    val orders: MutableList<Order> = mutableListOf()
    sort.forEach {
      if (it == null) {
        return@forEach
      }
      when (it.property) {
        "date" -> addOrderExpression(root.get(Milestone_.date), it.direction, cb, orders)
        "header" -> addOrderExpression(root.get(Milestone_.header), it.direction, cb, orders)
        "workArea" ->
            joinLeft(root, Milestone_.workArea).get().apply {
              addOrderExpression(this.get(WorkArea_.position), it.direction, cb, orders)
            }
        "position" -> addOrderExpression(root.get(Milestone_.position), it.direction, cb, orders)
        "createdDate" ->
            addOrderExpression(root.get(Milestone_.createdDate), it.direction, cb, orders)
        "type" -> addOrderExpression(root.get(Milestone_.type), it.direction, cb, orders)
        "id" -> addOrderExpression(root.get(AbstractPersistable_.id), ASC, cb, orders)
        else -> throw IllegalStateException("Unknown property: ${it.property}")
      }
    }

    return orders
  }

  private fun addOrderExpression(
      expression: Expression<*>,
      direction: Sort.Direction,
      cb: CriteriaBuilder,
      orders: MutableList<Order>
  ) {
    if (direction.isAscending) orders.add(cb.asc(expression)) else orders.add(cb.desc(expression))
  }
}
