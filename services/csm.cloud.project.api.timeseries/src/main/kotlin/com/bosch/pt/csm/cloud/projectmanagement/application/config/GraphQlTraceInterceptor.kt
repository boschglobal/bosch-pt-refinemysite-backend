/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.tracing.AbstractTraceInterceptor
import datadog.trace.api.interceptor.MutableSpan

class GraphQlTraceInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> =
      trace.filterNot {
        it.tags["graphql.type"]?.toString()?.contains("Payload") == true ||
            it.tags["graphql.coordinates"]?.toString()?.contains("Payload") == true
      }
}
