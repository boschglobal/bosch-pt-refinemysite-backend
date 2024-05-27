/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.exceptions

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/** Hamcrest matcher for [AggregateNotFoundException]. */
class ResourceNotFoundExceptionMatcher(private val messageKey: String) :
    TypeSafeMatcher<AggregateNotFoundException>() {

  override fun matchesSafely(item: AggregateNotFoundException): Boolean =
      item.messageKey == messageKey

  override fun describeMismatchSafely(
      item: AggregateNotFoundException,
      mismatchDescription: Description
  ) = mismatchDescription.appendText("was ").appendValue(item.messageKey).returnUnit()

  override fun describeTo(description: Description) =
      description.appendText("expects message key ").appendValue(messageKey).returnUnit()
}
