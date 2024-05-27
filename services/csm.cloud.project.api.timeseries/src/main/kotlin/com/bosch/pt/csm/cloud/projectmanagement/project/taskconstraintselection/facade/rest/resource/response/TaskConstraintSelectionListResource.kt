/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response

data class TaskConstraintSelectionListResource(
    val taskConstraints: List<TaskConstraintSelectionResource>
)
