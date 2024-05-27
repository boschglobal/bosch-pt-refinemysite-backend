/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto

import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty

data class OverridableTaskParametersDto(val workAreaId: WorkAreaIdOrEmpty?)
