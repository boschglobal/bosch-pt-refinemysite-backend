/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import org.springframework.stereotype.Component

@Component
class TaskPayloadAssembler {

  fun assemble(task: Task, critical: Boolean?): TaskPayloadV1 =
      TaskPayloadMapper.INSTANCE.fromTask(task, critical)
}
