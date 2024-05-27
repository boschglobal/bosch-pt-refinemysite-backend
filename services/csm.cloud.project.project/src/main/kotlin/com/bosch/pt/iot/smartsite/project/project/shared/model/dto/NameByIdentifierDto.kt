/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model.dto

import java.util.UUID

data class NameByIdentifierDto(val identifier: UUID, val name: String) {

  companion object {
    fun of(identifier: UUID, name: String) = NameByIdentifierDto(identifier, name)
  }
}
