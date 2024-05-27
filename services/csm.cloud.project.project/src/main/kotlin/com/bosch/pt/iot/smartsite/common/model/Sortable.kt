/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import java.lang.IllegalArgumentException

/** It should be used at enumeration to provide DB sorting. */
interface Sortable {

  /**
   * Returns the position. It is used to order elements.
   *
   * @return the position
   */
  fun getPosition(): Int

  companion object {

    /**
     * Returns the correlated enumeration for the given position.
     *
     * @param <T> the sortable type
     * @param sortableClass the type of [Sortable]s
     * @param position the position
     * @return the correlated enumeration for the given position </T>
     */
    @JvmStatic
    operator fun <T> get(sortableClass: Class<T>, position: Int?): T where
    T : Enum<*>,
    T : Sortable =
        (sortableClass.enumConstants as Array<T>).firstOrNull { status: T ->
          status.getPosition() == position
        }
            ?: throw IllegalArgumentException(
                "Cannot find enum of type ${sortableClass.name} for position $position")
  }
}
