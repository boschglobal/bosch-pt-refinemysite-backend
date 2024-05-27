/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.test

import com.azure.storage.blob.BlobServiceClientBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileUrlResource

class FileSourceArgumentsProvider : ArgumentsProvider {

  override fun provideArguments(context: ExtensionContext): Stream<Arguments> {

    // Extract container and file names from the annotation
    val annotation = context.element.get().getAnnotation(FileSource::class.java)
    val fileNamesFromAnnotation = annotation.files
    val containerName = annotation.container.removeSuffix("/")

    // Find missing files locally
    val notLocallyExistingFiles =
        fileNamesFromAnnotation.filter { !fileResource(containerName, it).exists() }

    // Load missing files from the blob storage
    if (notLocallyExistingFiles.isNotEmpty()) {

      // Construct the blob storage client for the specified connection string
      val blobServiceClient =
          BlobServiceClientBuilder().connectionString(connectionString).buildClient()

      // Download the files from the blob storage to the local directory
      notLocallyExistingFiles.forEach {

        // Create the parent directories (if not exist)
        val file = File("src/test/resources/$containerName/$it")
        file.parentFile.mkdirs()

        LOGGER.info("Download blob: $containerName/$it")

        // Download the file
        blobServiceClient
            .getBlobContainerClient(containerName)
            .getBlobClient(it)
            .downloadToFile(file.absolutePath, false)
      }
    }

    // Return the files as classpath resources
    return fileNamesFromAnnotation.map { Arguments.of(fileResource(containerName, it)) }.stream()
  }

  private fun fileResource(containerName: String, fileName: String): FileUrlResource {
    val file = File("src/test/resources/$containerName/$fileName")
    return FileUrlResource(file.path)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(FileSourceArgumentsProvider::class.java)

    // Take the connection string from the environment variable if set, otherwise read the
    // connection string from azure-cli
    private val connectionString: String by lazy {
      System.getenv("TEST_DATA_BLOB_STORAGE_CONNECTION_STRING")?.trim()
          ?: loadConnectionStringFromAzureCli()
    }

    private fun loadConnectionStringFromAzureCli(): String {
      val process =
          Runtime.getRuntime()
              .exec(
                  "az storage account show-connection-string --name ptcsmtestdata " +
                      "--subscription PT-BDO-OF-RefineMySite-Dev -o tsv")

      // If the az command to read the connection string fails with text on stderr,
      // then exit with the provided error message
      process.errorReader(StandardCharsets.UTF_8).readText().also { check(it.isEmpty()) { it } }

      // Remove the enclosing quotes.
      return process.inputReader(StandardCharsets.UTF_8).readText().trim()
    }
  }
}
