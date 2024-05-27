/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.db

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.sql.Driver
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@SmartSiteSpringBootTest
class DbSchemaVerificationIntegrationTest {

  private val schemaExporter = SchemaExporter()

  @Value("classpath:db/schema-mysql.sql") private lateinit var schemaMysql: Resource

  @Autowired
  private lateinit var containerEntityManagerFactory: LocalContainerEntityManagerFactoryBean

  @Test
  fun verifyMysqlSchemaUpToDate() {
    verifySchema(schemaMysql, MySQLDialect::class.java, org.mariadb.jdbc.Driver::class.java)
  }

  private fun verifySchema(
      referenceFile: Resource,
      dialect: Class<out Dialect>,
      driver: Class<out Driver>
  ) {
    val tempFile =
        File.createTempFile("RefinemySite-schema-verification", referenceFile.filename).apply {
          this.deleteOnExit()
        }

    schemaExporter.exportSchema(
        containerEntityManagerFactory, dialect, driver, tempFile.absolutePath)

    val generatedSchema = FileUtils.readFileToString(tempFile, UTF_8).replace("\r\n", "\n")
    val referenceSchema =
        FileUtils.readFileToString(referenceFile.file, UTF_8).replace("\r\n", "\n")

    assertThat(generatedSchema).isEqualTo(referenceSchema)
  }
}
