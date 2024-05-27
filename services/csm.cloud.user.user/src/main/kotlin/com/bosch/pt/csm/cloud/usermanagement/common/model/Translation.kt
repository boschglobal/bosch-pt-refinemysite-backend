/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.model

import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull

/** Embeddable object for translations. */
@Embeddable
@Suppress("SerialVersionUIDInSerializableClass")
class Translation() : Serializable {

  @Column(name = "locale", nullable = false) var locale: @NotNull String? = null

  @Column(name = "value", nullable = false) var value: @NotNull String? = null

  /**
   * Field initialising constructor.
   *
   * @param locale the locale of the translation
   * @param value the actual translation
   */
  constructor(locale: String, value: String) : this() {
    this.locale = locale
    this.value = value
  }
}
