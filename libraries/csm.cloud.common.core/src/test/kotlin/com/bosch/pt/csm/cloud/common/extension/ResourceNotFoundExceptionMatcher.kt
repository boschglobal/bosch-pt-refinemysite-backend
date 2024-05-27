/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.extension

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/** Hamcrest matcher for [AggregateNotFoundException]. */
class ResourceNotFoundExceptionMatcher

/**
 * Constructor.
 *
 * @param messageKey the message key
 */
constructor(private val messageKey: String) : TypeSafeMatcher<AggregateNotFoundException>() {

  override fun matchesSafely(item: AggregateNotFoundException): Boolean =
      item.messageKey == messageKey

  override fun describeMismatchSafely(
      item: AggregateNotFoundException,
      mismatchDescription: Description
  ) {
    mismatchDescription.appendText("was ").appendValue(item.messageKey)
  }

  override fun describeTo(description: Description) {
    description.appendText("expects message key ").appendValue(messageKey)
  }
}
