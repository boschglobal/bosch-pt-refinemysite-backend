/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.exceptions

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/** Hamcrest matcher for [PreconditionViolationException]. */
class PreconditionViolationExceptionMatcher(private val messageKey: String) :
    TypeSafeMatcher<PreconditionViolationException>() {

  override fun matchesSafely(item: PreconditionViolationException): Boolean =
      item.messageKey == messageKey

  override fun describeMismatchSafely(
      item: PreconditionViolationException,
      mismatchDescription: Description
  ) = mismatchDescription.appendText("was ").appendValue(item.messageKey).returnUnit()

  override fun describeTo(description: Description) =
      description.appendText("expects message key ").appendValue(messageKey).returnUnit()
}
