/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request

data class SearchProjectListResource(
    val title: String?,
    val company: String?,
    val creator: String?
)
