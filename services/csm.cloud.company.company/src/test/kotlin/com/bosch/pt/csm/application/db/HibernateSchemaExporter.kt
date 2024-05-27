/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.db

import com.bosch.pt.csm.application.SmartSiteSpringBootTest
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

/** Schema exporter for different database dialects. */
@SmartSiteSpringBootTest
class HibernateSchemaExporter {

  private val schemaExporter = SchemaExporter()

  @Autowired private lateinit var entityManagerFactory: LocalContainerEntityManagerFactoryBean

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
