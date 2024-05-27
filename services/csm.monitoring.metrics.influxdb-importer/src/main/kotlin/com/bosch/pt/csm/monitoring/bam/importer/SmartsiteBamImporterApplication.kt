package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.tracing.KafkaServiceRenamingInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication @EnableConfigurationProperties class SmartsiteBamImporterApplication

fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().addTraceInterceptor(KafkaServiceRenamingInterceptor(100))
  runApplication<SmartsiteBamImporterApplication>(*args)
}
