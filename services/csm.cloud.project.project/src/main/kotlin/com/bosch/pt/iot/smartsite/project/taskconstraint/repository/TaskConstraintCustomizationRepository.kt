/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.repository

import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph

interface TaskConstraintCustomizationRepository :
    KafkaStreamableRepository<
        TaskConstraintCustomization, Long, TaskConstraintCustomizationEventEnumAvro> {

  fun findOneByKeyAndProjectIdentifier(
      key: TaskConstraintEnum,
      projectIdentifier: ProjectId
  ): TaskConstraintCustomization?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): TaskConstraintCustomization?

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<TaskConstraintCustomization>

  fun findOneByIdentifier(identifier: UUID): TaskConstraintCustomization?
}
