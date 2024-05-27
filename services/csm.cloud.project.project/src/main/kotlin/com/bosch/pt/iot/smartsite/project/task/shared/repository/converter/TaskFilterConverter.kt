/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.repository.converter

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable_
import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler.join
import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler.joinLeft
import com.bosch.pt.iot.smartsite.company.model.Company_
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId_
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft_
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.COMPANY
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.END
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.LOCATION
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.NAME
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.PROJECT_CRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.START
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.STATUS
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.TOPIC
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskSearchOrderEnum.WORK_AREA
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task_
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule_
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic_
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId_
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea_
import com.bosch.pt.iot.smartsite.user.model.User_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.lang.Boolean.TRUE
import java.util.LinkedList
import java.util.UUID
import kotlin.Int.Companion.MAX_VALUE
import org.apache.commons.collections4.CollectionUtils.isEmpty
import org.apache.commons.collections4.CollectionUtils.isNotEmpty
import org.springframework.data.domain.Sort

object TaskFilterConverter {

  fun converterFilter(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>
  ): List<Predicate> {
    val predicates: MutableList<Predicate> = LinkedList()
    predicates.add(cb.equal(root.get(Task_.deleted), false))
    filterByAssignedParticipantsOrCompanies(taskFilterDto, cb, root, predicates)
    filterByProject(taskFilterDto, cb, root, predicates)
    filterByProjectCrafts(taskFilterDto, root, predicates)
    filterByStartAndEndDate(taskFilterDto, cb, root, predicates)
    filterByTaskStatus(taskFilterDto, cb, root, predicates)
    filterByTopicCriticality(taskFilterDto, root, predicates)
    filterByTopicsExist(taskFilterDto, cb, root, predicates)
    filterByWorkAreas(taskFilterDto, cb, root, predicates)
    filterByAllDaysInDateRange(taskFilterDto, cb, root, predicates)
    return predicates
  }

  private fun filterByAssignedParticipantsOrCompanies(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    val orPredicates: MutableList<Predicate> = LinkedList()
    filterByAssignedParticipants(taskFilterDto, root, orPredicates)
    filterByAssignedCompany(taskFilterDto, root, orPredicates)

    when (orPredicates.size) {
      0 -> return
      1 -> predicates.add(orPredicates[0])
      2 -> predicates.add(cb.or(*orPredicates.toTypedArray()))
      else -> throw IllegalStateException("Unexpected number of predicates: ${orPredicates.size}")
    }
  }

  private fun filterByAssignedCompany(
      taskFilterDto: TaskFilterDto,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (isNotEmpty(taskFilterDto.assignedCompanies)) {
      val participantCompanyJoinCompanyJoin =
          joinLeft(root, Task_.assignee).joinLeft(Participant_.company).get()

      predicates.add(
          participantCompanyJoinCompanyJoin
              .get(Company_.identifier)
              .`in`(taskFilterDto.assignedCompanies))
    }
  }

  private fun filterByAssignedParticipants(
      taskFilterDto: TaskFilterDto,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (isNotEmpty(taskFilterDto.assigneeIds)) {
      val taskParticipant = join(root, Task_.assignee).get()
      predicates.add(
          taskParticipant
              .join(Participant_.identifier)
              .get<UUID>(ParticipantId_.identifier.name)
              .`in`(taskFilterDto.assigneeIds?.map { it.identifier }))
    }
  }

  private fun filterByProject(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    val taskProjectJoin = join(root, Task_.project).get()
    predicates.add(
        cb.equal(
            taskProjectJoin.join(Project_.identifier).get<UUID>(ProjectId_.identifier.name),
            taskFilterDto.projectRef.identifier))
  }

  private fun filterByProjectCrafts(
      taskFilterDto: TaskFilterDto,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (isNotEmpty(taskFilterDto.projectCraftIds)) {
      val taskCraftJoin = join(root, Task_.projectCraft).get()

      predicates.add(
          taskCraftJoin
              .join(ProjectCraft_.identifier)
              .get<UUID>(ProjectId_.identifier.name)
              .`in`(taskFilterDto.projectCraftIds.map { it.identifier }))
    }
  }

  private fun filterByStartAndEndDate(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    val rangeStartDate = taskFilterDto.rangeStartDate
    val rangeEndDate = taskFilterDto.rangeEndDate
    val taskTaskScheduleJoin = joinLeft(root, Task_.taskSchedule).get()

    // join schedules to their project to support the MySQL query optimizer. Otherwise, the query
    // optimizer tends to evaluate schedule date ranges in the very end of the query evaluation,
    // doing a lot of useless joins of tasks that are not even in the requested date range.
    if (rangeStartDate != null || rangeEndDate != null) {
      predicates.add(
          cb.equal(
              joinLeft(root, Task_.taskSchedule)
                  .joinLeft(TaskSchedule_.project)
                  .get()
                  .join(Project_.identifier)
                  .get<UUID>(ProjectId_.identifier.name),
              taskFilterDto.projectRef.identifier))
    }

    if (rangeStartDate != null && rangeEndDate != null) {
      predicates.add(
          cb.or(
              cb.between(
                  taskTaskScheduleJoin.get(TaskSchedule_.start), rangeStartDate, rangeEndDate),
              cb.between(taskTaskScheduleJoin.get(TaskSchedule_.end), rangeStartDate, rangeEndDate),
              cb.and(
                  cb.lessThanOrEqualTo(
                      taskTaskScheduleJoin.get(TaskSchedule_.start), rangeStartDate),
                  cb.greaterThanOrEqualTo(
                      taskTaskScheduleJoin.get(TaskSchedule_.end), rangeEndDate)),
              cb.and(
                  cb.isNull(taskTaskScheduleJoin.get(TaskSchedule_.start)),
                  cb.greaterThanOrEqualTo(
                      taskTaskScheduleJoin.get(TaskSchedule_.end), rangeStartDate)),
              cb.and(
                  cb.isNull(taskTaskScheduleJoin.get(TaskSchedule_.end)),
                  cb.lessThanOrEqualTo(
                      taskTaskScheduleJoin.get(TaskSchedule_.start), rangeEndDate))))
    } else if (rangeStartDate != null) {

      predicates.add(
          cb.or(
              cb.greaterThanOrEqualTo(taskTaskScheduleJoin.get(TaskSchedule_.end), rangeStartDate),
              cb.isNull(taskTaskScheduleJoin.get(TaskSchedule_.end))))
    } else if (rangeEndDate != null) {

      predicates.add(
          cb.or(
              cb.lessThanOrEqualTo(taskTaskScheduleJoin.get(TaskSchedule_.start), rangeEndDate),
              cb.isNull(taskTaskScheduleJoin.get(TaskSchedule_.start))))
    }
    if (taskFilterDto.startAndEndDateMustBeSet != null &&
        taskFilterDto.startAndEndDateMustBeSet == TRUE) {

      predicates.add(
          cb.and(
              cb.isNotNull(taskTaskScheduleJoin.get(TaskSchedule_.start)),
              cb.isNotNull(taskTaskScheduleJoin.get(TaskSchedule_.end))))
    }
  }

  private fun filterByTaskStatus(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {

    // Filter for the task status or if the search list is null force an empty result with task
    // status null.
    if (isNotEmpty(taskFilterDto.taskStatus)) {

      // in case all status enums are selected, a filter predicate would have no effect.
      if (!taskFilterDto.taskStatus!!.containsAll(TaskStatusEnum.entries)) {
        predicates.add(root.get(Task_.status).`in`(taskFilterDto.taskStatus))
      }
    } else {

      predicates.add(cb.isNull(root.get(Task_.status)))
    }
  }

  private fun filterByTopicCriticality(
      taskFilterDto: TaskFilterDto,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (isNotEmpty(taskFilterDto.topicCriticality)) {
      val taskTopic = join(root, Task_.topics).get()
      predicates.add(taskTopic.get(Topic_.criticality).`in`(taskFilterDto.topicCriticality))
    }
  }

  private fun filterByTopicsExist(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (taskFilterDto.hasTopics != null) {
      predicates.add(
          if (taskFilterDto.hasTopics) cb.isNotEmpty(root.get(Task_.topics))
          else cb.isEmpty(root.get(Task_.topics)))
    }
  }

  private fun filterByWorkAreas(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    if (isEmpty(taskFilterDto.workAreaIds)) {
      return
    }

    val workAreaIds = taskFilterDto.workAreaIds.mapNotNull { it.identifier }

    var workAreaIdsPredicate: Predicate? = null
    if (workAreaIds.isNotEmpty()) {
      val taskWorkAreaJoin = joinLeft(root, Task_.workArea).get()
      workAreaIdsPredicate =
          taskWorkAreaJoin
              .join(WorkArea_.identifier)
              .get<UUID>(WorkAreaId_.identifier.name)
              .`in`(workAreaIds.map { it.identifier })
    }

    val includeEmpty = taskFilterDto.workAreaIds.any { it.isEmpty }
    var includeEmptyPredicate: Predicate? = null
    if (includeEmpty) {
      includeEmptyPredicate = root.get(Task_.workArea).isNull
    }

    if (workAreaIdsPredicate != null && includeEmptyPredicate != null) {

      predicates.add(cb.or(workAreaIdsPredicate, includeEmptyPredicate))
    } else if (workAreaIdsPredicate != null) {

      predicates.add(workAreaIdsPredicate)
    } else if (includeEmptyPredicate != null) {

      predicates.add(includeEmptyPredicate)
    }
  }

  private fun filterByAllDaysInDateRange(
      taskFilterDto: TaskFilterDto,
      cb: CriteriaBuilder,
      root: Root<Task>,
      predicates: MutableList<Predicate>
  ) {
    val rangeStartDate = taskFilterDto.rangeStartDate
    val rangeEndDate = taskFilterDto.rangeEndDate
    val taskTaskScheduleJoin = joinLeft(root, Task_.taskSchedule).get()

    if (taskFilterDto.allDaysInDateRange == true) {
      if (rangeStartDate != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(taskTaskScheduleJoin.get(TaskSchedule_.start), rangeStartDate))
      }
      if (rangeEndDate != null) {
        predicates.add(
            cb.lessThanOrEqualTo(taskTaskScheduleJoin.get(TaskSchedule_.end), rangeEndDate))
      }
    }
  }

  /** Converts a [Sort] to a list of [Order]. */
  fun applyOrder(sort: Sort?, cb: CriteriaBuilder, root: Root<Task>): List<Order> {
    if (sort == null) {
      return emptyList()
    }

    val orders: MutableList<Order> = LinkedList()
    val taskTaskScheduleJoin = joinLeft(root, Task_.taskSchedule).get()

    sort.forEach { order: Sort.Order? ->
      if (order == null) {
        return@forEach
      }

      val direction: Sort.Direction
      when (TaskSearchOrderEnum.of(order.property)) {
        NAME -> addOrderExpression(cb.upper(root.get(Task_.name)), order.direction, cb, orders)
        LOCATION ->
            addOrderExpression(cb.upper(root.get(Task_.location)), order.direction, cb, orders)
        START ->
            addOrderExpression(
                taskTaskScheduleJoin.get(TaskSchedule_.start), order.direction, cb, orders)
        END ->
            addOrderExpression(
                taskTaskScheduleJoin.get(TaskSchedule_.end), order.direction, cb, orders)
        PROJECT_CRAFT -> {
          val taskProjectCraftJoin = join(root, Task_.projectCraft).get()
          addOrderExpression(
              taskProjectCraftJoin.get(ProjectCraft_.position), order.direction, cb, orders)
        }
        COMPANY -> {
          val participantCompanyJoin =
              joinLeft(root, Task_.assignee).joinLeft(Participant_.company).get()

          val participantUserJoin = joinLeft(root, Task_.assignee).joinLeft(Participant_.user).get()

          direction = order.direction

          addOrderExpression(
              cb.upper(participantCompanyJoin.get(Company_.name)), direction, cb, orders)
          addOrderExpression(
              cb.upper(participantUserJoin.get(User_.firstName)), direction, cb, orders)
          addOrderExpression(
              cb.upper(participantUserJoin.get(User_.lastName)), direction, cb, orders)
        }
        STATUS -> addOrderExpression(root.get(Task_.status), order.direction, cb, orders)
        WORK_AREA -> {
          val taskWorkAreaJoin = joinLeft(root, Task_.workArea).get()

          addOrderExpressionNullsLast(
              taskWorkAreaJoin.get(WorkArea_.position), order.direction, cb, orders, MAX_VALUE)
        }
        TOPIC -> {
          val taskTopicJoin = joinLeft(root, Task_.topics).get()

          val critical =
              cb.sum(
                  cb.selectCase<Number>()
                      .`when`(cb.equal(taskTopicJoin.get(Topic_.criticality), CRITICAL), 1)
                      .otherwise(0))

          val uncritical =
              cb.sum(
                  cb.selectCase<Number>()
                      .`when`(cb.equal(taskTopicJoin.get(Topic_.criticality), UNCRITICAL), 1)
                      .otherwise(0))

          direction = order.direction
          addOrderExpression(critical, direction, cb, orders)
          addOrderExpression(uncritical, direction, cb, orders)
        }
      }
    }

    orders.add(cb.asc(root.get(AbstractPersistable_.id)))
    return orders
  }

  /**
   * Method to convert an expression in to and order with the correct direction and add it to the
   * list of orders.
   */
  private fun addOrderExpression(
      expression: Expression<*>,
      direction: Sort.Direction,
      cb: CriteriaBuilder,
      orders: MutableList<Order>
  ) {
    if (direction.isAscending) {
      orders.add(cb.asc(expression))
    } else {
      orders.add(cb.desc(expression))
    }
  }

  /**
   * Method to convert an expression in to and order with the correct direction and add it to the
   * list of orders. Use the given placeholder value to replace the null values on the order.
   */
  private fun addOrderExpressionNullsLast(
      expression: Expression<Int>,
      direction: Sort.Direction,
      cb: CriteriaBuilder,
      orders: MutableList<Order>,
      placeholder: Int
  ) {
    if (direction.isAscending) {
      orders.add(cb.asc(cb.coalesce(expression, placeholder)))
    } else {
      orders.add(cb.desc(cb.coalesce(expression, placeholder)))
    }
  }
}
