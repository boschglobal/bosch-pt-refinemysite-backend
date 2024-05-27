/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.shared.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
data class Holiday(
    @Column(nullable = false, length = MAX_NAME_LENGTH) var name: String,
    @Column(nullable = false) var date: LocalDate
) {
  companion object {
    const val MAX_NAME_LENGTH = 100

    /*
     Warning: Re-calculate the maximum amount of holidays if you change this class
     as it could exceed the maximum kafka message size when importing a project
     with many holiday exceptions.
    */
    const val MAX_HOLIDAY_AMOUNT = 10000
  }
}
