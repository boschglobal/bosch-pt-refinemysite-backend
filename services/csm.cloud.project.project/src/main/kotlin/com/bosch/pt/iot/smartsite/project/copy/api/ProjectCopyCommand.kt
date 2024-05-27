/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.api

import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyParameters
import java.util.Locale
import java.util.UUID

data class ProjectCopyCommand(
    val locale: Locale,
    val projectIdentifier: UUID,
    val copyParameters: ProjectCopyParameters
)
