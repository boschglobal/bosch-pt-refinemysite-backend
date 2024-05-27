/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@Embeddable
class PostBoxAddress : AbstractAddress {

  @field:NotNull
  @field:Size(min = 1, max = MAX_POSTBOX_LENGTH)
  @Column(length = MAX_POSTBOX_LENGTH)
  var postBox: String? = null

  constructor()

  constructor(
      city: String?,
      zipCode: String?,
      area: String?,
      country: String?,
      postBox: String?
  ) : super(city, zipCode, area, country) {
    this.postBox = postBox
  }

  override fun toString(): String =
      ToStringBuilder(this).appendSuper(super.toString()).append("postBox", postBox).toString()

  companion object {
    private const val serialVersionUID: Long = -8248577404211275314

    const val MAX_POSTBOX_LENGTH = 100
  }
}
