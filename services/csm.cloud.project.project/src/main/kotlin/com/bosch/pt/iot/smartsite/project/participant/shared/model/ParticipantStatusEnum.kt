/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.model

import com.bosch.pt.iot.smartsite.common.model.Sortable

enum class ParticipantStatusEnum(private val position: Int) : Sortable {
  INVITED(100),

  /**
   * the participant has finished his/her part of the registration process and is now awaiting
   * company assignment by the support team to became ACTIVE.
   */
  VALIDATION(200),
  ACTIVE(300),
  INACTIVE(400);

  override fun getPosition(): Int = position
}
