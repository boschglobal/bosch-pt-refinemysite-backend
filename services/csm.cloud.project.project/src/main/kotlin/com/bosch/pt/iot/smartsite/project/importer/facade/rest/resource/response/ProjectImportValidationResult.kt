/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType

class ProjectImportValidationResult(
    val type: ValidationResultType,
    val summary: String,
    val elements: List<String>
)
