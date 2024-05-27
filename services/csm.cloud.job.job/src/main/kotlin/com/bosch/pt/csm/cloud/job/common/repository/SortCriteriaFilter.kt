/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.job.common.repository

import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object SortCriteriaFilter {
  fun filterAndTranslate(
      pageable: Pageable,
      propertyTranslationMap: Map<String, String>
  ): Pageable {
    if (pageable.isUnpaged) {
      return pageable
    }

    val translatedOrders = pageable.sort.map { translate(it, propertyTranslationMap) }.flatten()

    return when (translatedOrders.isEmpty()) {
      true -> PageRequest.of(pageable.pageNumber, pageable.pageSize)
      false -> PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(translatedOrders))
    }
  }

  private fun translate(
      order: Sort.Order,
      propertyTranslationMap: Map<String, String>
  ): List<Sort.Order> =
      StringUtils.split(order.property, ",")
          .map { StringUtils.strip(it) }
          .filter { propertyTranslationMap.containsKey(it) }
          .map { Sort.Order(order.direction, propertyTranslationMap[it]!!) }
}
