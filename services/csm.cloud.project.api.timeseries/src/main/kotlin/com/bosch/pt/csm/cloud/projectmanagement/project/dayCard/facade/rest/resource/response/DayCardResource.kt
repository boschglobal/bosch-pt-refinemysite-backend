/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import java.math.BigDecimal
import java.time.LocalDate

data class DayCardResource(
    val id: DayCardId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val date: LocalDate,
    val status: String,
    val title: String,
    val manpower: BigDecimal,
    val notes: String? = null,
    val reason: String? = null,
    val deleted: Boolean,
    val eventTimestamp: Long
)
