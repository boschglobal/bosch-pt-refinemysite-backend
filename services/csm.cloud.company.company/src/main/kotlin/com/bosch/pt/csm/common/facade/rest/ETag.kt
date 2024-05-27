/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import org.apache.commons.lang3.StringUtils.wrap
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.util.StringUtils.trimLeadingCharacter
import org.springframework.util.StringUtils.trimTrailingCharacter

/**
 * ETag helper class inspired by implementation in spring data rest project. See
 * org.springframework.data.rest.webmvc.support.ETag.
 * org.springframework.data.rest.webmvc.support.ETag.
 */
class ETag private constructor(value: String) {

  private val value: String = trimTrailingCharacter(trimLeadingCharacter(value, '"'), '"')

  @ExcludeFromCodeCoverage override fun hashCode(): Int = value.hashCode()

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is ETag) {
      return false
    }
    return ObjectUtils.nullSafeEquals(value, other.value)
  }

  @ExcludeFromCodeCoverage override fun toString(): String = wrap(value, '"')

  @ExcludeFromCodeCoverage fun toVersion(): Long = value.toLong()

  companion object {

    /**
     * Creates a new [ETag] for the given [String] value.
     *
     * @param value the source ETag value, must not be null or empty.
     * @return an [ETag] instance from given string value
     */
    fun from(value: String): ETag {
      Assert.hasLength(value, "Value for ETag must not be empty!")
      return ETag(value)
    }

    fun from(value: Long): ETag {
      return ETag(value.toString())
    }
  }
}
