/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler
import com.bosch.pt.iot.smartsite.common.repository.JoinRecycler.join
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId_
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Root
import java.util.UUID
import org.springframework.data.jpa.domain.Specification

object TaskSpecifications {

  /** Filters tasks whose identifier is contained in the given collection of task identifiers. */
  @Suppress("FunctionNaming")
  fun `in`(taskIdentifiers: Collection<UUID>): Specification<Task> =
      Specification { task: Root<Task>, _: CriteriaQuery<*>, _: CriteriaBuilder ->
        task.join(Task_.identifier).get<UUID>(TaskId_.identifier.name).`in`(taskIdentifiers)
      }

  /** Filters task whose id is equal to the given task id. */
  fun equalsId(taskId: Long): Specification<Task> =
      Specification { task: Root<Task>, _: CriteriaQuery<*>, builder: CriteriaBuilder ->
        builder.equal(task.get(Task_.id), taskId)
      }

  /**
   * Filters tasks where the specified user is an active participant.
   *
   * @return the join object for the path `task.project.participants`.
   */
  @JvmStatic
  fun activeParticipant(
      userIdentifier: UUID,
      task: From<*, Task>,
      builder: CriteriaBuilder
  ): JoinRecycler.RecyclableJoin<Project, Participant> {
    join(task, Task_.project).join(Project_.participants).join(Participant_.user).get().apply {
      on(builder.equal(get<Any>("identifier"), userIdentifier))
    }
    join(task, Task_.project).join(Project_.participants).get().apply {
      on(builder.equal(get<Any>("status"), ACTIVE))
    }
    return join(task, Task_.project).join(Project_.participants)
  }
}
