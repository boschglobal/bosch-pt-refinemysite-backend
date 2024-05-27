/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import javax.sql.DataSource
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@Configuration
@EnableCaching
class JpaConfig(
    private val dataSource: DataSource,
    private val jpaProperties: JpaProperties,
) {

  @Bean fun jdbcTemplate() = JdbcTemplate(dataSource)

  @Bean
  fun entityManagerFactory(
      builder: EntityManagerFactoryBuilder
  ): LocalContainerEntityManagerFactoryBean =
      builder
          .dataSource(dataSource)
          .packages(
              "com.bosch.pt.csm.cloud.usermanagement.announcement.model",
              "com.bosch.pt.csm.cloud.usermanagement.common.model.converter",
              "com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model",
              "com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model",
              "com.bosch.pt.csm.cloud.usermanagement.consents.eventstore",
              "com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model",
              "com.bosch.pt.csm.cloud.usermanagement.craft.eventstore",
              "com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model",
              "com.bosch.pt.csm.cloud.usermanagement.pat.eventstore",
              "com.bosch.pt.csm.cloud.usermanagement.user.authorization.model",
              "com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model",
              "com.bosch.pt.csm.cloud.usermanagement.user.eventstore",
              "com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model")
          .properties(jpaProperties.properties)
          .build()
}
