package com.bosch.pt.csm.cloud.event

import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import com.bosch.pt.csm.cloud.event.application.tracing.DropUnwantedTracesInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication @EnableConfigurationProperties class EventApplication

fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(DropUnwantedTracesInterceptor(100))
    addTraceInterceptor(KafkaServiceRenamingInterceptor(101))
  }
  SpringApplication.run(EventApplication::class.java, *args)
}
