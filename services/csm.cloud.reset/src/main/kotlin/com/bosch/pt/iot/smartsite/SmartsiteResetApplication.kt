/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication @EnableConfigurationProperties class SmartsiteResetApplication

fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(KafkaServiceRenamingInterceptor(101))
    addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(102))
  }

  runApplication<SmartsiteResetApplication>(*args)
}
