/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory.dto

import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType

class TranslatedValidationResultDto(
    val type: ValidationResultType,
    val summary: String,
    val element: String?
)
