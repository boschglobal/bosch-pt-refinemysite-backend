/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.mongodb.extensions

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class MongoDbTestExtension : BeforeAllCallback {

  override fun beforeAll(context: ExtensionContext) {
    container.start()
    System.setProperty("spring.data.mongodb.uri", container.replicaSetUrl)
  }

  companion object {
    private val image = DockerImageName.parse("mongo").withTag("6.0")

    private var container: MongoDBContainer = MongoDBContainer(image).withReuse(reuse())

    // Enable reuse only if explicitly set. This only reuses the container, if the
    // $HOME/.testcontainers.properties is set up to allow reuse (testcontainers.reuse.enable=true)
    private fun reuse(): Boolean = System.getProperty("reuseContainer").toBoolean()
  }
}
