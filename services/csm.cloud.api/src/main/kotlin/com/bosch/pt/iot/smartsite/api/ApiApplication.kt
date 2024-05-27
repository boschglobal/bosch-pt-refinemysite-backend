/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.api.tracing.DropUnwantedSpansInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import java.util.TimeZone.getTimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication @EnableConfigurationProperties class ApiApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  TimeZone.setDefault(getTimeZone("UTC"))
  GlobalTracer.get().apply { addTraceInterceptor(DropUnwantedSpansInterceptor(100)) }
  runApplication<ApiApplication>(*args)
}
