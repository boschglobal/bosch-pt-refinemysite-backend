/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.etag

import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.util.StringUtils.trimLeadingCharacter
import org.springframework.util.StringUtils.trimTrailingCharacter

/**
 * ETag helper class inspired by implementation in spring data rest project. See
 * org.springframework.data.rest.webmvc.support.ETag.
 */
class ETag private constructor(value: String) {

  fun toVersion() = value.toLong()

  private val value: String = trimTrailingCharacter(trimLeadingCharacter(value, '"'), '"')

  override fun hashCode(): Int = value.hashCode()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is ETag) {
      return false
    }
    return ObjectUtils.nullSafeEquals(value, other.value)
  }

  override fun toString(): String = "\"$value\""

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
  }
}
