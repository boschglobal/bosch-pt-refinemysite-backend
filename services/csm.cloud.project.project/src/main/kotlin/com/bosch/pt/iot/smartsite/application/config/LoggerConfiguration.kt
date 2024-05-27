/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
open class LoggerConfiguration {

  @Bean
  @Scope("prototype")
  open fun logger(ip: InjectionPoint): Logger =
      ip.methodParameter?.containingClass.let { LoggerFactory.getLogger(it) }
          ?: throw IllegalStateException("Logger can not be configured")
}
