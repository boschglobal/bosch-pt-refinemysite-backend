/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectAddress
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectCategoryEnum
import java.time.LocalDate

class CreateProjectResource(
    val client: String? = null,
    val description: String? = null,
    val end: LocalDate? = null,
    val start: LocalDate? = null,
    val projectNumber: String? = null,
    val title: String? = null,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddress? = null
)
