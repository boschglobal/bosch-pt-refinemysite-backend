/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory.dto

import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType

data class ValidationResultTypeDto(val type: ValidationResultType, val summary: String)
