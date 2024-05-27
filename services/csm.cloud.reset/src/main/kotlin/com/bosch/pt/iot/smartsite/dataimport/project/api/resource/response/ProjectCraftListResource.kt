/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links", "_embedded")
class ProjectCraftListResource(
    val version: Long? = null,
    val projectCrafts: List<ProjectCraftResource> = emptyList()
)
