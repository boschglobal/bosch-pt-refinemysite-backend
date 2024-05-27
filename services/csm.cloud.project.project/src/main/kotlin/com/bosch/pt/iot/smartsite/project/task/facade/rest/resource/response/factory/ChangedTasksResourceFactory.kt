/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.ChangedTasksResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Deprecated("Should be replaced with TaskBatchResourceFactory")
@Transactional(readOnly = true)
@Component
open class ChangedTasksResourceFactory(private val factoryHelper: TaskResourceFactoryHelper) {

  /** Creates a [ChangedTasksResource] for a list of [Task] s. */
  open fun build(tasks: List<Task>, includeEmbedded: Boolean): ChangedTasksResource {
    val taskResources: Collection<TaskResource> =
        factoryHelper.build(tasks, includeEmbedded)

    return ChangedTasksResource(taskResources)
  }
}
