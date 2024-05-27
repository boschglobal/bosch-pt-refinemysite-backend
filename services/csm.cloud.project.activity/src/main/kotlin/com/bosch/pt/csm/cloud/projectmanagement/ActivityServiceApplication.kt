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
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone.getTimeZone
import java.util.TimeZone.setDefault
import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableConfigurationProperties(ApiVersionProperties::class)
@EnableCaching
class ActivityServiceApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  setDefault(getTimeZone("UTC"))
  GlobalTracer.get().addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
  run(ActivityServiceApplication::class.java, *args)
}
