/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedEventTableMetricsTracesInterceptor
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import com.bosch.pt.csm.cloud.usermanagement.application.tracing.DropUnwantedSpansInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.retry.annotation.EnableRetry

@EnableRetry(proxyTargetClass = true)
@EnableConfigurationProperties(ApiVersionProperties::class)
@SpringBootApplication(
    scanBasePackages = ["com.bosch.pt.csm.cloud.common", "com.bosch.pt.csm.cloud.usermanagement"])
class UserApplication

@ExcludeFromCodeCoverageGenerated
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  val tracer = GlobalTracer.get()
  tracer.addTraceInterceptor(DropUnwantedEventTableMetricsTracesInterceptor(100))
  tracer.addTraceInterceptor(KafkaServiceRenamingInterceptor(101))
  tracer.addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(102))
  tracer.addTraceInterceptor(DropUnwantedSpansInterceptor(103))

  // Send the ContextStartedEvent to ApplicationListener implementing classes
  SpringApplication.run(UserApplication::class.java, *args).start()
}
