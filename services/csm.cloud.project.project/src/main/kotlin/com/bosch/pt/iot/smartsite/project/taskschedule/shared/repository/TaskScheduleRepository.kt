/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskScheduleRepository :
    JpaRepository<TaskSchedule, Long>,
    TaskScheduleRepositoryExtension,
    JpaSpecificationExecutor<TaskSchedule> {

  @EntityGraph(attributePaths = ["slots.dayCard", "task", "project", "createdBy", "lastModifiedBy"])
  fun findWithDetailsByIdentifier(identifier: TaskScheduleId): TaskSchedule?

  fun findOneByTaskIdentifier(taskIdentifier: TaskId): TaskSchedule?

  @Query("select ts.identifier from TaskSchedule ts where ts.task.identifier = :identifier")
  fun findIdentifierByTaskIdentifier(@Param("identifier") identifier: TaskId): TaskScheduleId?

  @Query("select ts.id from TaskSchedule ts where ts.task.identifier = :identifier")
  fun findIdByTaskIdentifier(@Param("identifier") identifier: TaskId): Long?

  @EntityGraph(attributePaths = ["slots.dayCard", "task", "project", "createdBy", "lastModifiedBy"])
  fun findWithDetailsByTaskIdentifier(taskIdentifier: TaskId): TaskSchedule?

  @EntityGraph(attributePaths = ["slots.dayCard", "task", "project", "createdBy", "lastModifiedBy"])
  fun findWithDetailsByTaskIdentifierIn(taskIdentifier: Collection<TaskId>): List<TaskSchedule>

  fun <T> findAllByTaskIdentifierIn(
      taskIdentifiers: Collection<TaskId>,
      type: Class<T>
  ): Collection<T>

  @Query(
      "Select t.identifier from TaskSchedule ts join ts.task t where ts.identifier in (:identifiers)")
  fun findTaskIdentifiersByIdentifierIn(
      @Param("identifiers") identifiers: Collection<TaskScheduleId>
  ): Collection<TaskId>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto(" +
          "ts.identifier, " +
          "ts.version, ts.start, ts.end, " +
          "ts.createdBy, ts.createdDate, ts.lastModifiedBy, ts.lastModifiedDate, " +
          "t.identifier, t.name, t.project.identifier) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "where ts.identifier = :identifier " +
          "and t.project.identifier = :projectIdentifier")
  fun findEntityByIdentifierAndProjectIdentifier(
      identifier: TaskScheduleId,
      projectIdentifier: ProjectId
  ): TaskScheduleWithoutDayCardsDto?

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto(" +
          "ts.identifier, " +
          "ts.version, ts.start, ts.end, " +
          "ts.createdBy, ts.createdDate, ts.lastModifiedBy, ts.lastModifiedDate, " +
          "t.identifier, t.name, t.project.identifier) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "where ts.identifier in (:identifiers)")
  fun findTaskScheduleWithoutDayCardsDtosByIdentifiers(
      @Param("identifiers") identifiers: Collection<TaskScheduleId>
  ): Collection<TaskScheduleWithoutDayCardsDto>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto(" +
          "ts.identifier, " +
          "ts.version, ts.start, ts.end, " +
          "ts.createdBy, ts.createdDate, ts.lastModifiedBy, ts.lastModifiedDate, " +
          "t.identifier, t.name, t.project.identifier) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "where t.identifier in (:taskIdentifiers)")
  fun findTaskScheduleWithoutDayCardsDtosByTaskIdentifiers(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>
  ): Collection<TaskScheduleWithoutDayCardsDto>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto(" +
          "ts.identifier, " +
          "ts.version, ts.start, ts.end, " +
          "ts.createdBy, ts.createdDate, ts.lastModifiedBy, ts.lastModifiedDate, " +
          "t.identifier, t.name, t.project.identifier) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "where t.identifier = :taskIdentifier")
  fun findTaskScheduleWithoutDayCardsDtoByTaskIdentifier(
      @Param("taskIdentifier") taskIdentifier: TaskId
  ): TaskScheduleWithoutDayCardsDto?

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto(" +
          "ts.identifier, " +
          "s.date, " +
          "d.identifier, d.version, d.title, d.manpower, d.notes, d.status, d.reason," +
          "d.createdBy, d.createdDate, d.lastModifiedBy, d.lastModifiedDate) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "join ts.slots s " +
          "join s.dayCard d " +
          "where t.identifier in (:taskIdentifiers)")
  fun findAllDayCardsByTaskIdentifierIn(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>
  ): Collection<TaskScheduleSlotWithDayCardDto>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto(" +
          "ts.identifier, " +
          "s.date, " +
          "d.identifier, d.version, d.title, d.manpower, d.notes, d.status, d.reason, " +
          "d.createdBy, d.createdDate, d.lastModifiedBy, d.lastModifiedDate) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "join ts.slots s " +
          "join s.dayCard d " +
          "where ts.identifier in (:identifiers)")
  fun findAllDayCardsByIdentifierIn(
      @Param("identifiers") identifiers: Collection<TaskScheduleId>
  ): Collection<TaskScheduleSlotWithDayCardDto>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto(" +
          "ts.identifier, " +
          "s.date, " +
          "d.identifier, d.version, d.title, d.manpower, d.notes, d.status, d.reason, " +
          "d.createdBy, d.createdDate, d.lastModifiedBy, d.lastModifiedDate) " +
          "from TaskSchedule ts " +
          "join ts.task t " +
          "join ts.slots s " +
          "join s.dayCard d " +
          "where d.identifier in (:identifiers)")
  fun findAllDayCardsByDayCardIdentifierIn(
      @Param("identifiers") identifiers: Collection<DayCardId>
  ): Collection<TaskScheduleSlotWithDayCardDto>
}
