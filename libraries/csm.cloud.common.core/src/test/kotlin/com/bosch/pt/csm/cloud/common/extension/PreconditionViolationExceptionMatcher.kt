/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.extension

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/** Hamcrest matcher for [PreconditionViolationException]. */
class PreconditionViolationExceptionMatcher

/**
 * Constructor.
 *
 * @param messageKey the message key
 */
constructor(private val messageKey: String) : TypeSafeMatcher<PreconditionViolationException>() {

  override fun matchesSafely(item: PreconditionViolationException): Boolean =
      item.messageKey == messageKey

  override fun describeMismatchSafely(
      item: PreconditionViolationException,
      mismatchDescription: Description
  ) {
    mismatchDescription.appendText("was ").appendValue(item.messageKey)
  }

  override fun describeTo(description: Description) {
    description.appendText("expects message key ").appendValue(messageKey)
  }
}
