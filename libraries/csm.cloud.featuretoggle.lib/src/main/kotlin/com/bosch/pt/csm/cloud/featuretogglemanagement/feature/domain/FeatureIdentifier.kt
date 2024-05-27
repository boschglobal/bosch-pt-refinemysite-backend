/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class FeatureIdentifier(@get:JsonValue val value: UUID) {

  constructor(uuidAsString: String) : this(UUID.fromString(uuidAsString))

  override fun toString() = value.toString()
}
