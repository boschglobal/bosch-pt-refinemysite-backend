/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApiVersionProperties::class)
class ProjectNewsApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  GlobalTracer.get().apply {
    addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
    addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(101))
  }

  runApplication<ProjectNewsApplication>(*args)
}
