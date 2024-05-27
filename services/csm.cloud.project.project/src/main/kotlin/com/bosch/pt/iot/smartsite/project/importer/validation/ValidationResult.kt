/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.validation

class ValidationResult(
    val type: ValidationResultType,
    val element: String?,
    val messageKey: String,
    vararg val messageArguments: String
)

enum class ValidationResultType {
  INFO,
  ERROR
}
