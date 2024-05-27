/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.db

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.mariadb.jdbc.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@SmartSiteSpringBootTest
class HibernateSchemaExporter {

  private val schemaExporter = SchemaExporter()

  @Autowired
  private lateinit var containerEntityManagerFactory: LocalContainerEntityManagerFactoryBean

  @Test
  fun exportMySqlSchema() {
    schemaExporter.exportSchema(
        containerEntityManagerFactory,
        MySQLDialect::class.java,
        Driver::class.java,
        "src/main/resources/db/schema-mysql.sql")
  }
}
