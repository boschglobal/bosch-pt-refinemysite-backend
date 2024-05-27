/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.repository

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobProperties
import com.bosch.pt.csm.cloud.common.blob.model.MalwareScanResult.NOT_SCANNED
import com.bosch.pt.csm.cloud.common.extensions.AzureBlobStorageWithQuarantineTestExtension
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.test.annotation.DirtiesContext

@ExtendWith(AzureBlobStorageWithQuarantineTestExtension::class)
@SpringBootTest
@DirtiesContext
class AzureBlobStorageRepositoryTest {

  @Autowired lateinit var repository: AzureBlobStorageRepository

  @Autowired lateinit var quarantineRepository: QuarantineBlobStorageRepository

  @Test
  fun `find returns requested blob`() {
    val blobName = "file"
    val data = "data".toByteArray()
    quarantineRepository.apply {
      save(data, blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    val blob = repository.find(blobName)!!
    assertThat(blob.blobName).isEqualTo(blobName)
    assertThat(blob.data).isEqualTo(data)
    assertThat(blob.mimeType).isEqualTo(TEXT_PLAIN_VALUE)
    assertThat(blob.metadata.toMap()).isEqualTo(mapOf("trace_header" to "--1-"))
  }

  @Test
  fun `find blob returns null if the blob does not exist`() {
    assertThat(repository.find(randomUUID().toString())).isNull()
  }

  @Test
  fun `findProperties returns blob properties`() {
    val blobName = "directory/some-file"
    quarantineRepository.apply {
      save("My test content".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    val blobMetadata = repository.findProperties(blobName)

    assertThat(blobMetadata).isEqualTo(BlobProperties(blobSize = 15))
  }

  @Test
  fun `get malware scan result`() {
    val blobName = "file"
    quarantineRepository.apply {
      save("data".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    assertThat(repository.getMalwareScanResult(blobName)).isEqualTo(NOT_SCANNED)
  }

  @Test
  fun `read returns blob content as stream`() {
    val blobName = "directory/blob"
    quarantineRepository.apply {
      save("Hello Blob!".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    val blobLines = repository.read(blobName)!!.bufferedReader().readLines()

    assertThat(blobLines).isEqualTo(listOf("Hello Blob!"))
  }

  @Test
  fun `read returns null if blob does not exist`() {
    val blobLines = repository.read("non-existent/blob")

    assertThat(blobLines).isNull()
  }

  @Test
  fun `deleteBlobsInDirectory delete all blobs`() {
    val blobName1 = "directory/file1"
    val blobName2 = "directory/file2"
    quarantineRepository.apply {
      save("1".toByteArray(), blobName1, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      save("2".toByteArray(), blobName2, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName1, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
      moveFromQuarantine(
          blobName2, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    repository.deleteBlobsInDirectory("directory/")

    assertThat(repository.find(blobName1)).isNull()
    assertThat(repository.find(blobName2)).isNull()
  }

  @Test
  fun `deleteBlobsInDirectory does not delete excluded blob`() {
    val blobName1 = "directory/file1"
    val blobName2 = "directory/file2"
    quarantineRepository.apply {
      save("1".toByteArray(), blobName1, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      save("2".toByteArray(), blobName2, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName1, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
      moveFromQuarantine(
          blobName2, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    repository.deleteBlobsInDirectory("directory/", blobName1)

    assertThat(repository.find(blobName1)).isNotNull
    assertThat(repository.find(blobName2)).isNull()
  }

  @Test
  fun `deleteIfExists deletes existing blob`() {
    val blobName = "directory/blob"
    quarantineRepository.apply {
      save("Hello Blob!".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    assertThat(repository.deleteIfExists(blobName)).isTrue()
    assertThat(repository.find(blobName)).isNull()
  }

  @Test
  fun `deleteIfExists ignores non-existing blob`() {
    val blobName = "not-exists"
    assertThat(repository.find(blobName)).isNull()
    assertThat(repository.deleteIfExists(blobName)).isFalse()
  }

  @Test
  fun `generateSignedUrl throws when expiry is in past`() {
    assertThatIllegalArgumentException()
        .isThrownBy { repository.generateSignedUrl(randomUUID().toString(), -10) }
        .withMessageContaining("must be larger than 0")
  }

  @Test
  fun `generateSignedUrl returns null if blob does not exist`() {
    assertThat(repository.generateSignedUrl(randomUUID().toString())).isEqualTo(null)
  }

  @Test
  fun `generateSignedUrl returns url for blob`() {
    val blobName = randomUUID().toString()
    quarantineRepository.apply {
      save("data".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }
    assertThat(repository.generateSignedUrl(blobName).toString()).contains(blobName)
  }

  @Test
  fun `generateSignedUploadUrl and upload to new blob succeeds`() {
    val blobName = randomUUID().toString()
    val uploadUrl = repository.generateSignedUploadUrlForInternalUse(blobName)

    val statusCode = upload(uploadUrl, "hello world")
    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `generateSignedUploadUrl falls back to configuration when no expiry is set`() {
    val blobName = randomUUID().toString()

    val uploadUrl = repository.generateSignedUploadUrlForInternalUse(blobName)
    val statusCode = upload(uploadUrl, "hello world 3")

    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `generateSignedUploadUrlInternal and overwriting existing blob succeeds`() {
    val blobName = randomUUID().toString()
    upload(repository.generateSignedUploadUrlForInternalUse(blobName), "hello world")

    val uploadUrl = repository.generateSignedUploadUrlForInternalUse(blobName)

    val statusCode = upload(uploadUrl, "hello world 2")
    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `generateSignedUploadUrlInternal throws when expiry is in past`() {
    assertThatIllegalArgumentException()
        .isThrownBy {
          repository.generateSignedUploadUrlForInternalUse(randomUUID().toString(), -10)
        }
        .withMessageContaining("must be larger than 0")
  }

  @Test
  fun `getBlockBlobClient returns client for existing blob`() {
    val blobName = randomUUID().toString()
    quarantineRepository.apply {
      save("data".toByteArray(), blobName, TEXT_PLAIN_VALUE, BlobMetadata.fromMap())
      moveFromQuarantine(
          blobName, repository.blobContainerClient, removeQuarantineDirectoryPrefix = false)
    }

    assertThat(repository.getBlockBlobClient(blobName).exists()).isTrue()
  }

  @Test
  fun `getBlockBlobClient returns client for non-existing blob`() {
    assertThat(repository.getBlockBlobClient(randomUUID().toString()).exists()).isFalse()
  }

  private fun upload(uploadUrl: URL, content: String): Int {
    val connection: HttpURLConnection = uploadUrl.openConnection() as HttpURLConnection

    try {
      connection.apply {
        doOutput = true
        requestMethod = "PUT"
        setRequestProperty("Content-Type", "text/plain")
        setRequestProperty("date", System.currentTimeMillis().toString())
        setRequestProperty("x-ms-blob-type", "BlockBlob")
        connect()
      }

      connection.outputStream.use { it.write(content.toByteArray()) }
      return connection.responseCode
    } finally {
      connection.disconnect()
    }
  }
}
