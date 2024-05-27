/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.tracing

import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory

class DropUnwantedMySqlSpansInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    val filteredTrace: MutableList<MutableSpan> = mutableListOf()
    for (span in trace) {
      if (!shouldDropMySqlQuery(span)) {
        filteredTrace.add(span)
      } else {
        if (LOGGER.isDebugEnabled) {
          LOGGER.debug("Dropping span: {}", span)
        }
      }
    }
    return filteredTrace
  }

  private fun shouldDropMySqlQuery(span: MutableSpan): Boolean {
    if (!isMySqlQuery(span)) {
      return false
    }
    val resourceName = span.resourceName.toString()
    return (resourceName.equals("commit", ignoreCase = true) ||
        resourceName.equals("set autocommit=0", ignoreCase = true) ||
        resourceName.equals("set autocommit=1", ignoreCase = true) ||
        resourceName.equals("select @@transaction_isolation", ignoreCase = true) ||
        resourceName.equals("set names utf8mb4 COLLATE utf8mb4_unicode_ci", ignoreCase = true) ||
        resourceName.equals("select version()", ignoreCase = true))
  }

  private fun isMySqlQuery(span: MutableSpan) = span.operationName.toString() == "mysql.query"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DropUnwantedMySqlSpansInterceptor::class.java)
  }
}
