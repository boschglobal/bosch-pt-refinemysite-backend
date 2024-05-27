/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ProjectCraftResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class TaskResource(
    val id: UUID? = null,
    val project: ResourceReference? = null,
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val assignee: ResourceReferenceWithPicture? = null,
    val creator: ResourceReferenceWithPicture? = null,
    val projectCraft: ProjectCraftResource? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val editDate: Date? = null,
    val status: String? = null,
    val company: ResourceReference? = null,
    val workArea: ResourceReference? = null,
    val assigned: Boolean = false,
    val statistics: TaskStatisticsResource? = null
) : AuditableResource()
