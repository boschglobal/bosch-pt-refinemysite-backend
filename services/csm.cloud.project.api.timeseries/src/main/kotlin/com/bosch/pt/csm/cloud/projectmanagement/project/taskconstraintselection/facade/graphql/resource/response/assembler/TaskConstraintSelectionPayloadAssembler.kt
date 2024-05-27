/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.TaskConstraintSelectionPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import org.springframework.stereotype.Component

@Component
class TaskConstraintSelectionPayloadAssembler {

  fun assemble(constraintSelection: TaskConstraintSelection): TaskConstraintSelectionPayloadV1 =
      TaskConstraintSelectionPayloadMapper.INSTANCE.fromTaskConstraintSelection(constraintSelection)
}
