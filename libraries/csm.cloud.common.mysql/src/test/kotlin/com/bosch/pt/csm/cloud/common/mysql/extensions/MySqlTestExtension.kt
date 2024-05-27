/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.mysql.extensions

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName

class MySqlTestExtension : BeforeAllCallback {

  override fun beforeAll(context: ExtensionContext) {
    // Read database name from @Tag Annotation. If not present use "default"
    // If multiple services have not set a @Tag annotation for the tests, this will cause flyway to
    // crash
    val databaseName = context.tags.firstOrNull() ?: "default"
    container.withDatabaseName(databaseName).start()
    System.setProperty("spring.datasource.url", container.jdbcUrl)
  }

  companion object {
    private val image = DockerImageName.parse("mysql:8.0").asCompatibleSubstituteFor("mariadb")
    private var container: MariaDBContainer<*> =
        MariaDBContainer(image)
            .withUsername("test")
            .withPassword("test")
            .withReuse(reuse())
            .withCommand("--log_bin_trust_function_creators=ON")
            // permitMySqlScheme is needed to allow the MariaDB driver to work with mysql. Usually
            // there would be no value, but this is not supported for testcontainers
            .withUrlParam("permitMysqlScheme", "true")
            .withUrlParam("allowPublicKeyRetrieval", "true")

    // Enable reuse only if explicitly set. This only reuses the container, if the
    // $HOME/.testcontainers.properties is set up to allow reuse (testcontainers.reuse.enable=true)
    private fun reuse(): Boolean = System.getProperty("reuseContainer").toBoolean()
  }
}
