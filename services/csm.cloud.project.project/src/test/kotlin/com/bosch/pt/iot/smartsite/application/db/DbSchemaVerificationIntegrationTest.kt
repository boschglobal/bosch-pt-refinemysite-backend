/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.db

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.sql.Driver
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.MySQLDialect
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@SmartSiteSpringBootTest
class DbSchemaVerificationIntegrationTest {

  private val schemaExporter = SchemaExporter()

  @Value("classpath:db/schema-h2.sql") private lateinit var schemaH2: Resource

  @Value("classpath:db/schema-mysql.sql") private lateinit var schemaMysql: Resource

  @Autowired private lateinit var entityManagerFactory: LocalContainerEntityManagerFactoryBean

  @Test
  fun verifyH2SchemaUpToDate() {
    verifySchema(schemaH2, H2Dialect::class.java, org.h2.Driver::class.java)
  }

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
        File.createTempFile("rms-schema-verification", referenceFile.filename).apply {
          deleteOnExit()
        }

    schemaExporter.exportSchema(entityManagerFactory, dialect, driver, tempFile.absolutePath)

    var generatedSchema = FileUtils.readFileToString(tempFile, UTF_8)
    generatedSchema = StringUtils.replace(generatedSchema, "\r\n", "\n")

    var referenceSchema = FileUtils.readFileToString(referenceFile.file, UTF_8)
    referenceSchema = StringUtils.replace(referenceSchema, "\r\n", "\n")

    assertThat(generatedSchema).isEqualTo(referenceSchema)
  }
}
