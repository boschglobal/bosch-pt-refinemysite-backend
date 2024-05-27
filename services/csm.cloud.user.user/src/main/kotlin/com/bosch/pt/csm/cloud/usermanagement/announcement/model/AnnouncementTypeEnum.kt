/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import com.bosch.pt.csm.cloud.usermanagement.common.model.Sortable

enum class AnnouncementTypeEnum(private val position: Int) : Sortable {
  SUCCESS(30),
  NEUTRAL(40),
  WARNING(20),
  ERROR(10);

  override fun getPosition(): Int {
    return position
  }
}
