/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.model

import java.util.EnumMap

class DayCardStatusCountAggregation {
  val countByStatus: MutableMap<DayCardStatusEnum, Long> = EnumMap(DayCardStatusEnum::class.java)

  constructor()

  constructor(status: DayCardStatusEnum, count: Long) {
    countByStatus[status] = count
  }
}
