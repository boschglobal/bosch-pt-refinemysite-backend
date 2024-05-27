/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model.dto

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.Date

data class DateByProjectIdentifierDto(val projectIdentifier: ProjectId, val date: Date)
