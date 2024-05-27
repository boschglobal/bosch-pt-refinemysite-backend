/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import org.springframework.stereotype.Component

@Component
class DayCardAttributeSorter {

  fun sortAttributes(attributes: Set<String>) =
      attributes.sortedWith(
          Comparator { a, b -> attributeSortIndex(a).compareTo(attributeSortIndex(b)) })

  @ExcludeFromCodeCoverage
  private fun attributeSortIndex(attribute: String?) =
      when (attribute) {
        Key.DAY_CARD_ATTRIBUTE_TITLE -> 1
        Key.DAY_CARD_ATTRIBUTE_MANPOWER -> 2
        Key.DAY_CARD_ATTRIBUTE_NOTES -> 3
        else -> 100
      }
}
