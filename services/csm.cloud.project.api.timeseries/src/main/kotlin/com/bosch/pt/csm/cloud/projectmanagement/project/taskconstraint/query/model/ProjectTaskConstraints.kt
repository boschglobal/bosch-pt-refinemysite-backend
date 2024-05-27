/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId

data class ProjectTaskConstraints(val projectId: ProjectId, val constraints: List<TaskConstraint>)
