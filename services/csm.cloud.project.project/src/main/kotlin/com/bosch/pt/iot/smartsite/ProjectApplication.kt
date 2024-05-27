/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedEventTableMetricsTracesInterceptor
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import com.bosch.pt.iot.smartsite.application.tracing.DropUnwantedSpansInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@EnableConfigurationProperties(ApiVersionProperties::class)
@SpringBootApplication(
    scanBasePackages = ["com.bosch.pt.csm.cloud.common", "com.bosch.pt.iot.smartsite"])
open class ProjectApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(DropUnwantedEventTableMetricsTracesInterceptor(100))
    addTraceInterceptor(KafkaServiceRenamingInterceptor(101))
    addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(102))
    addTraceInterceptor(DropUnwantedSpansInterceptor(103))
  }
  // Send the ContextStartedEvent to ApplicationListener implementing classes
  SpringApplication.run(ProjectApplication::class.java, *args).start()
}
