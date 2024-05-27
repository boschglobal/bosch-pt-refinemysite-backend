/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.db

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import org.h2.Driver
import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.test.context.TestPropertySource

/** Schema exporter for different database dialects. */
@SmartSiteSpringBootTest
@TestPropertySource(
    properties =
        [
            "spring.flyway.enabled=false",
            "hibernate.hbm2ddl.auto=auto",
            "custom.business-transaction.consumer.persistence=jpa"])
class HibernateSchemaExporter {

  private val schemaExporter = SchemaExporter()

  @Autowired private lateinit var entityManagerFactory: LocalContainerEntityManagerFactoryBean

  /** Export schema for h2 database. */
  @Test
  fun exportH2Schema() {
    schemaExporter.exportSchema(
        entityManagerFactory,
        H2Dialect::class.java,
        Driver::class.java,
        "src/main/resources/db/schema-h2.sql")
  }

  /** Export schema for mysql database. */
  @Test
  fun exportMySqlSchema() {
    schemaExporter.exportSchema(
        entityManagerFactory,
        MySQLDialect::class.java,
        org.mariadb.jdbc.Driver::class.java,
        "src/main/resources/db/schema-mysql.sql")
  }
}
