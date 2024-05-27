/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common

import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsResource
import org.assertj.core.api.ListAssert
import org.assertj.core.api.RecursiveComparisonAssert

fun ListAssert<NewsResource>.matchesNewsInAnyOrder(
    vararg expectedNews: NewsResource
): RecursiveComparisonAssert<*> =
    this.containsExactlyInAnyOrder(*expectedNews)
        .usingRecursiveComparison()
        .ignoringFields("createdDate", "lastModifiedDate")
