/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.domain

data class WorkAreaIdOrEmpty(val identifier: WorkAreaId? = null) {

  val isEmpty: Boolean
    get() = identifier == null

  companion object {

    const val EMPTY_REPRESENTATION: String = "EMPTY"
  }
}
