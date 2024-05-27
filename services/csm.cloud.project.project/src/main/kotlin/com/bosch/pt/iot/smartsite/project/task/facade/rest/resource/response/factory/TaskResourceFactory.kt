/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class TaskResourceFactory(private val factoryHelper: TaskResourceFactoryHelper) {

  open fun build(task: Task): TaskResource = factoryHelper.build(listOf(task), true).first()
}
