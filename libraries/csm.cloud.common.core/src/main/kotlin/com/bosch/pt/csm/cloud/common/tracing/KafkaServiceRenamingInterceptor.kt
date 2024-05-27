/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.tracing

import datadog.trace.api.interceptor.MutableSpan

/**
 * Datadog uses the service name "kafka" for all spans that children of a Kafka listener (except for
 * database calls, HTTP calls etc.). This interceptor renames all but the first of these spans to
 * the actual service name. The first span, which is the entry point, keeps its "kafka" service
 * name.
 */
class KafkaServiceRenamingInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  private val serviceName: String = System.getenv("DD_SERVICE") ?: "undefined"

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    trace.filter { it.serviceName == "kafka" && it.spanType != "queue" }.forEach {
      it.serviceName = serviceName
    }
    return trace
  }
}
