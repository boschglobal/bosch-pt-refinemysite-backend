/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import com.bosch.pt.csm.cloud.projectmanagement.application.config.GraphQlTraceInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication(
    scanBasePackages =
        ["com.bosch.pt.csm.cloud.common", "com.bosch.pt.csm.cloud.projectmanagement"])
class ProjectTimeSeriesApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
    addTraceInterceptor(GraphQlTraceInterceptor(101))
  }
  // Send the ContextStartedEvent to ApplicationListener implementing classes
  SpringApplication.run(ProjectTimeSeriesApplication::class.java, *args).start()
}
