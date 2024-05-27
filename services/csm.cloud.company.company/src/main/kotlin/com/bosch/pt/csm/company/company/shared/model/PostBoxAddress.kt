/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@Embeddable
class PostBoxAddress : AbstractAddress() {

  // Post box
  @field:Size(min = 1, max = MAX_POSTBOX_LENGTH)
  @Column(length = MAX_POSTBOX_LENGTH)
  lateinit var postBox: String

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this).appendSuper(super.toString()).append("postBox", postBox).toString()

  companion object {
    const val MAX_POSTBOX_LENGTH = 100
  }
}
