/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request

import java.util.UUID

class CreateWorkAreaResource(
    val projectId: UUID? = null,
    val name: String? = null,
    val position: Int? = null
)
