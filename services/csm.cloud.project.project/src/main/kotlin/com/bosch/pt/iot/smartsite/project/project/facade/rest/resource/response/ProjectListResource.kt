/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractPageResource

class ProjectListResource(

    // Projects
    val projects: Collection<ProjectResource>,

    // This is currently a workaround to distinguish if a user was "activated" yet or not by being
    // assigned to a company. This can later be replaced when implementing the invitation flow.
    val userActivated: Boolean,

    // Page attributes
    pageNumber: Int,
    pageSize: Int,
    totalPages: Int,
    totalElements: Long
) : AbstractPageResource(pageNumber, pageSize, totalPages, totalElements)
