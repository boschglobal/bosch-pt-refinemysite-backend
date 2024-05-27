/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.db

import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@SmartSiteSpringBootTest
class HibernateSchemaExporter {

  private val schemaExporter = SchemaExporter()

  @Autowired private lateinit var bean: LocalContainerEntityManagerFactoryBean

  @Test
  fun exportMySqlSchema() {
    schemaExporter.exportSchema(
        bean,
        MySQLDialect::class.java,
        org.mariadb.jdbc.Driver::class.java,
        "src/main/resources/db/schema-mysql.sql")
  }
}
