/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import javax.sql.DataSource
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@EnableSchedulerLock(defaultLockAtMostFor = "1h")
@ConditionalOnProperty(value = ["custom.scheduling.enabled"], havingValue = "true")
@Configuration
class SchedulerLockConfiguration {

  @Bean
  fun lockProvider(dataSource: DataSource): LockProvider =
      JdbcTemplateLockProvider(
          JdbcTemplateLockProvider.Configuration.builder()
              .withJdbcTemplate(JdbcTemplate(dataSource))
              .usingDbTime()
              .build())
}
