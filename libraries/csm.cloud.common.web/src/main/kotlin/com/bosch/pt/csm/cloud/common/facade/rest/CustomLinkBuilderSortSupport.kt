/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import org.springframework.data.domain.Sort
import org.springframework.data.web.SortHandlerMethodArgumentResolverSupport

/**
 * Allows to make use of [SortHandlerMethodArgumentResolverSupport.foldIntoExpressions] logic.
 */
class CustomLinkBuilderSortSupport : SortHandlerMethodArgumentResolverSupport() {

  /**
   * Resolves a [Sort] into query parameter value expressions.
   */
  fun resolveSort(sort: Sort): List<String> {
    return super.foldIntoExpressions(sort)
  }
}
