/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

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
class JpaEntityConfiguration(
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
              "com.bosch.pt.csm.cloud.common.model.converter",
              "com.bosch.pt.csm.cloud.featuretoggle.eventstore",
              "com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model",
              "com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model",
              "com.bosch.pt.csm.cloud.user.query")
          .properties(jpaProperties.properties)
          .build()
}
