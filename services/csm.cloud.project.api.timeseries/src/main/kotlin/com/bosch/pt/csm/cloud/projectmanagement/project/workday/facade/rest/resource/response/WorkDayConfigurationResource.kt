/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.WorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.Holiday

data class WorkDayConfigurationResource(
    val id: WorkDayConfigurationId,
    val version: Long,
    val project: ProjectId,
    val startOfWeek: String,
    val workingDays: List<String>,
    val holidays: List<Holiday>,
    val allowWorkOnNonWorkingDays: Boolean,
    val deleted: Boolean,
    val eventTimestamp: Long
)
