/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.kafkaconnector

import com.bosch.pt.csm.cloud.common.kafkaconnector.tracing.DropUnwantedTracesInterceptor
import com.bosch.pt.csm.cloud.common.tracing.DropUnwantedMySqlSpansInterceptor
import datadog.trace.api.GlobalTracer
import java.util.TimeZone
import kotlin.system.exitProcess
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableScheduling

@Suppress("UtilityClassWithPublicConstructor")
@SpringBootApplication
@EnableScheduling
class KafkaConnectorApplication {

  companion object {
    var context: ConfigurableApplicationContext? = null

    fun restart() {
      val args = context!!.getBean(ApplicationArguments::class.java)
      val thread = Thread {
        context!!.close()
        context = runApplication<KafkaConnectorApplication>(*args.sourceArgs)
      }
      thread.isDaemon = false
      thread.start()
    }

    fun stop() {
      exitProcess(1)
    }
  }
}

fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  GlobalTracer.get().apply {
    addTraceInterceptor(DropUnwantedTracesInterceptor(100))
    addTraceInterceptor(DropUnwantedMySqlSpansInterceptor(101))
  }
  KafkaConnectorApplication.context = runApplication<KafkaConnectorApplication>(*args)
}
