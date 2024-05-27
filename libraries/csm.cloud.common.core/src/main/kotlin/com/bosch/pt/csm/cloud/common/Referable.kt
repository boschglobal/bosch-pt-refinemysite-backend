/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common

import java.util.UUID

/** Describes an object that can be referred to by a UUID identifier and a name to display */
interface Referable {

  /**
   * Returns the UUID identifier of the referred object.
   *
   * @return the UUID
   */
  fun getIdentifierUuid(): UUID

  /**
   * Returns a human-readable name of the referred object
   *
   * @return the display name
   */
  fun getDisplayName(): String?
}
