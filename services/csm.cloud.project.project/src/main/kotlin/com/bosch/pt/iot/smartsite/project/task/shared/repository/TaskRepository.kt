/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskAuthorizationDto
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskRepository :
    JpaRepository<Task, Long>, TaskRepositoryExtension, JpaSpecificationExecutor<Task> {

  fun findAllByIdentifierIn(taskIdentifiers: Collection<TaskId>): Collection<Task>

  fun findOneByIdentifier(identifier: TaskId): Task?

  @Query("select t.id from Task t where t.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: TaskId): Long?

  fun existsByIdentifierAndProjectIdentifier(
      identifier: TaskId,
      projectIdentifier: ProjectId
  ): Boolean

  fun existsByIdentifierInAndProjectIdentifier(
      identifiers: Collection<TaskId>,
      projectIdentifier: ProjectId
  ): Boolean

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "projectCraft.createdBy",
              "projectCraft.lastModifiedBy",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllWithDetailsByIdentifierIn(taskIdentifiers: Collection<TaskId>): List<Task>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "projectCraft.createdBy",
              "projectCraft.lastModifiedBy",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllWithDetailsByIdIn(taskIds: Collection<Long>): List<Task>

  // ToDo: [SMAR-18555] Add ticket number
  // When we migrate the User to Arch 2.0,
  // ce.user.identifier = t.createdBy.identifier is
  // going to be like the following
  // ce.user.identifier(UserId) = t.createdBy(UserId)
  @Query(
      "select new com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskAuthorizationDto(" +
          "t.identifier, t.status, t.project.identifier, a.identifier, ac.identifier, t.createdBy, " +
          "(select ce.company.identifier from Employee ce where ce.user.identifier = t.createdBy.identifier)) " +
          "from Task t " +
          "left join t.assignee a " +
          "left join a.company ac " +
          "where t.identifier in :taskIdentifiers")
  fun findAllForAuthorizationByIdentifierIn(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>
  ): Set<TaskAuthorizationDto>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft",
              "taskSchedule",
              "workArea"])
  fun findAllForCalendarByIdentifierIn(taskIdentifiers: List<TaskId>): List<Task>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: TaskId): Task?

  @Query("select t.id from Task t join t.project p where p.id = :id")
  fun findIdsByProjectId(@Param("id") id: Long): List<Long>

  @Query("select t.identifier from Attachment a join a.task t join t.project p where p.id = :id")
  fun findIdentifiersOfTasksWithAttachmentsByProjectId(@Param("id") id: Long): List<TaskId>

  @Query("select t.project.identifier from Task t where t.identifier = :identifier")
  fun findProjectIdentifierByIdentifier(identifier: TaskId): ProjectId

  fun existsByProjectCraftIdentifier(projectCraftIdentifier: ProjectCraftId): Boolean

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllByProjectIdentifier(projectIdentifier: ProjectId, pageable: Pageable): Page<Task>

  @EntityGraph(
      attributePaths =
          [
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<Task>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllByProjectIdentifierAndAssigneeIdentifier(
      projectIdentifier: ProjectId,
      assigneeIdentifier: ParticipantId,
      pageable: Pageable
  ): Page<Task>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllByProjectIdentifierAndAssigneeCompanyIdentifier(
      projectIdentifier: ProjectId,
      companyIdentifier: UUID,
      pageable: Pageable
  ): Page<Task>

  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "projectCraft.project",
              "project",
              "workArea",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy"])
  fun findAllWithDetailsByIdentifierInAndProjectIdentifier(
      identifiers: Set<TaskId>,
      projectIdentifier: ProjectId
  ): List<Task>

  // FIXME: This query fetches task schedules individually
  @EntityGraph(
      attributePaths =
          [
              "assignee.company",
              "assignee.user.profilePicture",
              "workArea",
              "projectCraft",
              "taskSchedule",
              "taskSchedule.createdBy",
              "taskSchedule.lastModifiedBy",
              "taskSchedule.slots"])
  fun findAllWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): List<Task>

  fun <T> findAllByIdentifierInAndProjectIdentifier(
      identifiers: Set<TaskId>,
      projectIdentifier: ProjectId,
      type: Class<T>
  ): List<T>

  fun countByProjectIdentifier(projectIdentifier: ProjectId): Long
}
