/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import javax.sql.DataSource
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@Configuration
@ConfigurationProperties(prefix = "persistenceunit")
@EnableCaching
class JpaEntityConfiguration(
    private val dataSource: DataSource,
    private val jpaProperties: JpaProperties,
) {

  /**
   * Adds [JdbcTemplate] bean to the application context.
   *
   * @return the [JdbcTemplate]
   */
  @Bean fun jdbcTemplate(): JdbcTemplate = JdbcTemplate(dataSource)

  /**
   * Initialize the entity manager factory bean.
   *
   * @param builder factory builder for entity manager
   * @return entity manager factory bean
   */
  @Bean(name = ["entityManagerFactory"])
  fun customerEntityManagerFactory(
      builder: EntityManagerFactoryBuilder
  ): LocalContainerEntityManagerFactoryBean =
      builder
          .dataSource(dataSource)
          .packages(
              "com.bosch.pt.csm.common.model.converter",
              "com.bosch.pt.csm.company.company.shared.model",
              "com.bosch.pt.csm.company.employee.query.employableuser",
              "com.bosch.pt.csm.company.employee.shared.model",
              "com.bosch.pt.csm.company.eventstore",
              "com.bosch.pt.csm.user.authorization.model",
              "com.bosch.pt.csm.user.picture.model",
              "com.bosch.pt.csm.user.user.model",
              "com.bosch.pt.csm.user.user.query")
          .properties(jpaProperties.properties)
          .build()
}
