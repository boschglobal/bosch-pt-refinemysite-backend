/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model.dto

import java.util.UUID

data class EmployeeDto(val identifier: UUID, val companyName: String, val userIdentifier: UUID)
