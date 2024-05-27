/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull

/** Embeddable object for translations. */
@Embeddable
class Translation : Serializable {

  @field:NotNull @Column(name = "locale", nullable = false) var locale: String? = null

  @field:NotNull @Column(name = "value", nullable = false) var value: String? = null

  constructor()

  /**
   * Field initialising constructor.
   *
   * @param locale the locale of the translation
   * @param value the actual translation
   */
  constructor(locale: String, value: String) {
    this.locale = locale
    this.value = value
  }

  companion object {
    private const val serialVersionUID: Long = 7916520600665778416
  }
}
