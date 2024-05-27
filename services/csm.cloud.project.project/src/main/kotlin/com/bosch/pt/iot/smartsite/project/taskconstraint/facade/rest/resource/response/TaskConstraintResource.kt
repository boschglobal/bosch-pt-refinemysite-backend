/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum

class TaskConstraintResource(val key: TaskConstraintEnum, val active: Boolean, val name: String) :
    AbstractResource() {

  companion object {
    const val LINK_CONSTRAINT_ACTIVATE = "activate"
    const val LINK_CONSTRAINT_DEACTIVATE = "deactivate"
    const val LINK_CONSTRAINT_UPDATE = "update"
    const val LINK_UPDATE_CONSTRAINTS = "updateConstraints"
  }
}
