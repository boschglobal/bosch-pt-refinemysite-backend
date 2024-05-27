/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.job

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
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
@SpringBootApplication(
    scanBasePackages = ["com.bosch.pt.csm.cloud.common", "com.bosch.pt.csm.cloud.job"])
class JobApplication

@ExcludeFromCodeCoverageGenerated
fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
  SpringApplication.run(JobApplication::class.java, *args).start()
}
