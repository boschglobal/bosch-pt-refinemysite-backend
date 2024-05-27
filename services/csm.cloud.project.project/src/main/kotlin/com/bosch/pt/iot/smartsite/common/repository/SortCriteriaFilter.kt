/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Order

object SortCriteriaFilter {

  @JvmOverloads
  fun filterAndTranslate(
      pageable: Pageable,
      propertyTranslationMap: Map<String, String>,
      addIdColumnAsLastSortCriteria: Boolean = true
  ): Pageable =
      if (pageable.isUnpaged) {
        pageable
      } else {
        val translatedOrders =
            pageable
                .sort
                .map { order -> translate(order, propertyTranslationMap) }
                .flatten()
                .toMutableList()
        if (addIdColumnAsLastSortCriteria) translatedOrders.add(Order(ASC, "id"))
        PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(translatedOrders))
      }

  private fun translate(order: Order, propertyTranslationMap: Map<String, String>) =
      order.property.split(",").map(String::trim).filter(propertyTranslationMap::containsKey).map {
        Order(order.direction, propertyTranslationMap[it]!!)
      }
}
