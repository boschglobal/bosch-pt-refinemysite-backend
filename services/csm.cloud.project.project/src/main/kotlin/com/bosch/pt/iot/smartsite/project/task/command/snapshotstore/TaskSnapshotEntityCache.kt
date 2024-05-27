/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class TaskSnapshotEntityCache(private val repository: TaskRepository) :
    AbstractSnapshotEntityCache<TaskId, Task>() {

  override fun loadOneFromDatabase(identifier: TaskId) = repository.findOneByIdentifier(identifier)

  open fun loadAllFromDatabase(identifiers: List<TaskId>) =
      repository.findAllByIdentifierIn(identifiers)
}
