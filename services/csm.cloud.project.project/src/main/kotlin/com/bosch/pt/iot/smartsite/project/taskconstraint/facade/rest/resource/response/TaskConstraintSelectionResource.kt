/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.NamedEnumReference
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

class TaskConstraintSelectionResource(
    @JsonProperty("taskId") val taskIdentifier: UUID,
    val items: List<NamedEnumReference<TaskConstraintEnum>>,
    val version: Long
) : AbstractResource() {

  companion object {
    const val LINK_CONSTRAINTS_UPDATE = "update"
  }
}
