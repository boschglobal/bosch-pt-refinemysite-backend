/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import java.util.Locale

/** Interface to be implemented by user objects to represent the locale string of the user. */
interface UserLocale {
  /**
   * Get the locale of the user or null if not set.
   *
   * @return the locale or null
   */
  fun getUserLocale(): Locale?
}
