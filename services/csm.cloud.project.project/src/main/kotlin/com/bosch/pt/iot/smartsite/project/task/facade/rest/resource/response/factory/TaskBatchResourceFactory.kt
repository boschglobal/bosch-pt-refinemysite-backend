/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.springframework.stereotype.Component

@Component
open class TaskBatchResourceFactory(private val factoryHelper: TaskResourceFactoryHelper) {

  open fun build(tasks: List<Task>, projectId: ProjectId) =
      BatchResponseResource(items = factoryHelper.build(tasks, false))
}
