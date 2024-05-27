/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedEventTableMetricsTracesInterceptor
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@EnableRetry
@EnableScheduling
@EnableConfigurationProperties(ApiVersionProperties::class)
@ExcludeFromCodeCoverage
@SpringBootApplication
class FeatureToggleApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(DropUnwantedEventTableMetricsTracesInterceptor(100))
    addTraceInterceptor(KafkaServiceRenamingInterceptor(101))
    addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(102))
  }

  // Send the ContextStartedEvent to ApplicationListener implementing classes
  SpringApplication.run(FeatureToggleApplication::class.java, *args).start()
}
