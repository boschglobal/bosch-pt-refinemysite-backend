/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model.dto

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

data class IdentifierAndNameByProjectIdentifierDto(
    val projectIdentifier: ProjectId,
    val identifier: UUID,
    val name: String
)
