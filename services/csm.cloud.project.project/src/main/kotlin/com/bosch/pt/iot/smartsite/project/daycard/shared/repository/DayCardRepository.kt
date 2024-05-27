/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.repository

import com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegationDto
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountProjection
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DayCardRepository : JpaRepository<DayCard, Long>, DayCardRepositoryExtension {

  fun findEntityByIdentifier(dayCardIdentifier: DayCardId): DayCard?

  @Query("select dc.id from DayCard dc where dc.identifier = :identifier")
  fun findOneByIdentifier(@Param("identifier") identifier: DayCardId): Long?

  @EntityGraph(
      attributePaths =
          ["createdBy", "lastModifiedBy", "taskSchedule.task", "taskSchedule.task.project"])
  fun findAllEntitiesWithDetailsByIdentifierIn(dayCardIdentifiers: Set<DayCardId>): Set<DayCard>

  @EntityGraph(
      attributePaths =
          ["createdBy", "lastModifiedBy", "taskSchedule.task", "taskSchedule.task.project"])
  fun findEntityWithDetailsByIdentifier(dayCardIdentifier: DayCardId): DayCard?

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto(" +
          "d.identifier, d.version, d.title, d.manpower, d.notes, d.status, d.reason, " +
          "d.createdBy, d.createdDate, d.lastModifiedBy, d.lastModifiedDate, ts.task.identifier, " +
          "ts.task.name, ts.task.project.identifier) " +
          "from DayCard d join d.taskSchedule ts join ts.task t " +
          "where d.identifier in (:dayCardIdentifier)")
  fun findAllByIdentifierIn(
      @Param("dayCardIdentifier") dayCardIdentifier: Set<DayCardId>
  ): List<DayCardDto>

  @EntityGraph(
      attributePaths =
          [
              "createdBy",
              "lastModifiedBy",
              "taskSchedule.slots",
              "taskSchedule.task.createdBy",
              "taskSchedule.task.lastModifiedBy",
              "taskSchedule.task.project"])
  @Query(
      "select dayCard from DayCard dayCard " +
          "join dayCard.taskSchedule as schedule " +
          "join schedule.slots as slots " +
          "where slots.dayCard = dayCard " +
          "and schedule.task.identifier = :taskIdentifier")
  fun findAllEntitiesWithDetailsByTaskIdentifier(
      @Param("taskIdentifier") taskIdentifier: TaskId
  ): Set<DayCard>

  @EntityGraph(
      attributePaths =
          [
              "createdBy",
              "lastModifiedBy",
              "taskSchedule.slots",
              "taskSchedule.task.createdBy",
              "taskSchedule.task.lastModifiedBy",
              "taskSchedule.task.project"])
  @Query(
      "select dayCard from DayCard dayCard " +
          "join dayCard.taskSchedule as schedule " +
          "join schedule.slots as slots " +
          "where slots.dayCard = dayCard " +
          "and schedule.task.identifier in :taskIdentifiers")
  fun findAllEntitiesWithDetailsByTaskIdentifierIn(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>
  ): Set<DayCard>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountProjection(" +
          "t.identifier, d.status, count(d.id)) " +
          "from DayCard d join d.taskSchedule ts join ts.task t " +
          "where t.identifier in (:taskIds)" +
          "group by t.identifier, d.status")
  fun countStatusGroupedByTaskIdentifier(
      @Param("taskIds") taskIdentifiers: Collection<TaskId>
  ): List<DayCardStatusCountProjection>

  @Query(
      "select dayCard.taskSchedule.task.identifier from DayCard dayCard " +
          "where dayCard.identifier = :dayCardIdentifier")
  fun findDayCardTaskIdentifierByDayCardIdentifier(
      @Param("dayCardIdentifier") dayCardIdentifier: DayCardId
  ): TaskId?

  @Query(
      "select new com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegationDto(" +
          "dayCard.identifier.identifier, dayCard.taskSchedule.task.identifier.identifier) " +
          "from DayCard dayCard " +
          "where dayCard.identifier in (:dayCardIdentifiers)")
  fun findDayCardTaskIdentifiersByDayCardIdentifiers(
      @Param("dayCardIdentifiers") dayCardIdentifiers: Collection<DayCardId>
  ): Set<AuthorizationDelegationDto>

  @Query(
      "select dayCard.taskSchedule.task.project.identifier from DayCard dayCard " +
          "where dayCard.identifier = :dayCardIdentifier")
  fun findDayCardProjectIdentifierByDayCardIdentifier(
      @Param("dayCardIdentifier") dayCardIdentifier: DayCardId
  ): ProjectId?

  @Query(
      "select distinct dayCard.taskSchedule.task.project.identifier from DayCard dayCard " +
          "where dayCard.identifier in :dayCardIdentifiers")
  fun findDayCardProjectIdentifiersByDayCardIdentifiers(
      @Param("dayCardIdentifiers") dayCardIdentifiers: Collection<DayCardId>
  ): Set<ProjectId>

  fun findAllByTaskScheduleProjectIdentifier(projectIdentifier: ProjectId): List<DayCard>
}
