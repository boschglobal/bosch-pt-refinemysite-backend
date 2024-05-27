/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links", "_embedded")
class WorkAreaListResource(
    val version: Long? = null,
    val workAreas: List<WorkAreaResource> = emptyList()
)
