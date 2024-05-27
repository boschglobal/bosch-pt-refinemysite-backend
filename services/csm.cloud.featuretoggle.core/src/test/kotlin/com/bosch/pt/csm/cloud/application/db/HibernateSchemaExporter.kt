/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.db

import com.bosch.pt.csm.cloud.application.RmsSpringBootTest
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.test.context.TestPropertySource

@RmsSpringBootTest
@TestPropertySource(properties = ["spring.flyway.enabled=false", "hibernate.hbm2ddl.auto=auto"])
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
