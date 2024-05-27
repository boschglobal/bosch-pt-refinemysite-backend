/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.model

import java.util.Arrays

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
    operator fun <T> get(sortableClass: Class<T>, position: Int): T where
    T : Enum<*>,
    T : Sortable =
        Arrays.stream(sortableClass.enumConstants as Array<T>)
            .filter { it.getPosition() == position }
            .findFirst()
            .orElseThrow {
              IllegalArgumentException(
                  "Cannot find enum of type ${sortableClass.name} for position $position")
            }
  }
}
