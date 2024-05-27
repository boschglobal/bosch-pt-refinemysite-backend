/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2017 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.model

import com.bosch.pt.iot.smartsite.common.model.Sortable

enum class ParticipantRoleEnum(private val position: Int) : Sortable {

  /** Company representative role. */
  CR(200),

  /** Construction site manager role. */
  CSM(100),

  /** Foreman role. */
  FM(300);

  override fun getPosition(): Int = position
}
