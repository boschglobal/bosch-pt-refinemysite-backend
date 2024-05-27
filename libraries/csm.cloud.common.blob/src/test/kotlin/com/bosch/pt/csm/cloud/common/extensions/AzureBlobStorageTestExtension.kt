/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.extensions

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import java.net.ServerSocket
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class AzureBlobStorageWithQuarantineTestExtension : AzureBlobStorageTestExtension(true)

open class AzureBlobStorageTestExtension(private val withQuarantine: Boolean = false) :
    BeforeAllCallback {

  private val blobStorageDockerImage =
      DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:${System.getProperty("azurite.version")}")

  private fun blobStorageContainerCommand(blobPort: Int) =
      "azurite -l /data -d /workspace/debug.log --blobHost 0.0.0.0 --queueHost 0.0.0.0 " +
          "--blobPort $blobPort --loose --disableProductStyleUrl"

  override fun beforeAll(context: ExtensionContext) {
    if (context.container == null) {
      val projectAccount = AzuriteAccount("projectaccount", "YmltbW9kZWxza2V5Cg==")
      val quarantineAccount = AzuriteAccount("quarantineaccount", "cXVhcmFudGluZWFjY291bnQK")

      val azuriteAccounts = buildString {
        append(projectAccount)
        if (withQuarantine) append(";").append(quarantineAccount)
      }

      val blobPort = randomUnusedPort()
      logger.info("Starting Blob storage container ...")
      val blobStorageContainer =
          GenericContainer(blobStorageDockerImage)
              .withCommand(blobStorageContainerCommand(blobPort))
              .withEnv("AZURITE_ACCOUNTS", azuriteAccounts)
              .withExposedPorts(blobPort)
              .withCreateContainerCmdModifier {
                // bind blob port to same port on host
                // this is needed, because moveFromQuarantine generates source url
                // (which contains host port), but azurite cannot access host port. Setting host
                // port and exposed port to same value works because url uses host port which is the
                // same as exposed port.
                it.hostConfig!!.withPortBindings(
                    PortBinding(Ports.Binding.bindPort(blobPort), ExposedPort(blobPort)))
              }
              .apply { start() }

      val blobConnectionString = buildConnectionString(blobStorageContainer, projectAccount)
      setSystemProperty("custom.blob-storage.connection-string", blobConnectionString)

      if (withQuarantine) {
        val quarantineConnectionString =
            buildConnectionString(blobStorageContainer, quarantineAccount)
        setSystemProperty("custom.quarantine-storage.connection-string", quarantineConnectionString)
        setSystemProperty("custom.quarantine-storage.directory", "images")
      }

      context.container = blobStorageContainer
    }
  }

  private var ExtensionContext.container: GenericContainer<*>?
    get() = getStore(GLOBAL).get("BLOB_STORAGE_CONTAINER", GenericContainer::class.java)
    set(value) = getStore(GLOBAL).put("BLOB_STORAGE_CONTAINER", value)

  private fun setSystemProperty(propertyName: String, propertyValue: String) {
    logger.info("Setting '$propertyName' property to $propertyValue ...")
    System.setProperty(propertyName, propertyValue)
  }

  private fun buildConnectionString(container: GenericContainer<*>, account: AzuriteAccount) =
      buildString {
        append("DefaultEndpointsProtocol=").append("http").append(";")
        append("AccountName=").append(account.accountName).append(";")
        append("AccountKey=").append(account.accountKey).append(";")
        append("BlobEndpoint=")
            .append("http://")
            .append(container.host)
            .append(":")
            .append(container.firstMappedPort)
            .append("/")
            .append(account.accountName)
      }

  private fun randomUnusedPort(): Int = ServerSocket(0).use { it.localPort }

  private companion object {
    val logger: Logger = LoggerFactory.getLogger(AzureBlobStorageTestExtension::class.java)
  }
}

private class AzuriteAccount(val accountName: String, val accountKey: String) {
  override fun toString() = "$accountName:$accountKey"
}
