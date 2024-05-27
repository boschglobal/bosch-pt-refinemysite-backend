/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.security.CustomWebSecurityAutoConfiguration
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone.getTimeZone
import java.util.TimeZone.setDefault
import org.springframework.boot.SpringApplication.run
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication(
    exclude =
        [
            SecurityAutoConfiguration::class,
            ManagementWebSecurityAutoConfiguration::class,
            CustomWebSecurityAutoConfiguration::class,
        ])
@EnableConfigurationProperties(ApiVersionProperties::class)
class PdfConverterApplication

@ExcludeFromCodeCoverage
fun main(args: Array<String>) {
  setDefault(getTimeZone("UTC"))
  GlobalTracer.get().addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
  run(PdfConverterApplication::class.java, *args)
}
