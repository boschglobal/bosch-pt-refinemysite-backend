/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectRoleEnum

class AssignProjectParticipantResource(
    val email: String? = null,
    val role: ProjectRoleEnum? = null
)
