/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.db

import com.bosch.pt.csm.cloud.application.RmsSpringBootTest
import java.io.File.createTempFile
import java.nio.charset.StandardCharsets
import java.sql.Driver
import org.apache.commons.io.FileUtils.readFileToString
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@RmsSpringBootTest
class DbSchemaVerificationIntegrationTest {

  private val schemaExporter = SchemaExporter()

  @Value("classpath:db/schema-mysql.sql") private lateinit var schemaMysql: Resource

  @Autowired private lateinit var bean: LocalContainerEntityManagerFactoryBean

  @Test
  fun verifyMysqlSchemaUpToDate() {
    verifySchema(schemaMysql, MySQLDialect::class.java, org.mariadb.jdbc.Driver::class.java)
  }

  private fun verifySchema(
      referenceFile: Resource,
      dialect: Class<out Dialect>,
      driver: Class<out Driver>
  ) {
    val tempFile = createTempFile("rms-schema-verification", referenceFile.filename)
    tempFile.deleteOnExit()

    schemaExporter.exportSchema(bean, dialect, driver, tempFile.absolutePath)

    var generatedSchema = readFileToString(tempFile, StandardCharsets.UTF_8)
    generatedSchema = StringUtils.replace(generatedSchema, "\r\n", "\n")

    var referenceSchema = readFileToString(referenceFile.file, StandardCharsets.UTF_8)
    referenceSchema = StringUtils.replace(referenceSchema, "\r\n", "\n")

    assertThat(generatedSchema).isEqualTo(referenceSchema)
  }
}
