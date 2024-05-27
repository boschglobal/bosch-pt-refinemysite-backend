/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskconstraint.repository

import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionConstraintsProjection
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionRootProjection
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskConstraintSelectionRepository :
    KafkaStreamableRepository<TaskConstraintSelection, Long, TaskActionSelectionEventEnumAvro>,
    TaskConstraintSelectionRepositoryExtension {

  @EntityGraph(attributePaths = ["createdBy", "lastModifiedBy", "task", "constraints"])
  fun findOneWithDetailsByTaskIdentifier(taskIdentifier: TaskId?): TaskConstraintSelection?

  @EntityGraph(attributePaths = ["createdBy", "lastModifiedBy", "task", "constraints"])
  fun findOneWithDetailsByIdentifier(identifier: UUID?): TaskConstraintSelection?

  @EntityGraph(attributePaths = ["task", "constraints"])
  fun findAllWithDetailsByTaskIdentifierIn(
      taskIdentifiers: Set<TaskId>
  ): List<TaskConstraintSelection>

  fun findTaskConstraintSelectionRootProjectionByTaskIdentifierIn(
      taskIdentifiers: Set<TaskId>
  ): List<TaskConstraintSelectionRootProjection>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.taskconstraint." +
          "model.dto.TaskConstraintSelectionConstraintsProjection(selection.identifier, action) " +
          "from TaskConstraintSelection selection " +
          "inner join selection.constraints action " +
          "where selection.task.identifier in :taskIdentifiers")
  fun findConstraintSelectionConstraintProjectionByTaskIdentifierIn(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>
  ): List<TaskConstraintSelectionConstraintsProjection>
}
