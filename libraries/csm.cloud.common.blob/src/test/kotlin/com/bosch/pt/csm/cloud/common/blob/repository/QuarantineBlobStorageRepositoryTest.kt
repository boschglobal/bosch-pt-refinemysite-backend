/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.repository

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.Companion.KEY_FILENAME
import com.bosch.pt.csm.cloud.common.blob.repository.AbstractAzureBlobStorageRepository.Companion.TRACE_SAMPLING_STATE_ACCEPT
import com.bosch.pt.csm.cloud.common.config.QuarantineBlobStorageProperties
import com.bosch.pt.csm.cloud.common.extensions.AzureBlobStorageWithQuarantineTestExtension
import io.opentracing.util.GlobalTracer
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.annotation.DirtiesContext

@ExtendWith(AzureBlobStorageWithQuarantineTestExtension::class)
@SpringBootTest
@DirtiesContext
class QuarantineBlobStorageRepositoryTest {

  @Autowired lateinit var repository: AzureBlobStorageRepository
  @Autowired lateinit var quarantineRepository: QuarantineBlobStorageRepository

  @Test
  fun `save with filename from metadata`() {
    val blobName = randomUUID().toString()
    val blobMetadata = BlobMetadata.fromMap(mapOf(KEY_FILENAME to "file.json"))
    val data = "{...}".toByteArray(UTF_8)
    val blob = quarantineRepository.save(data, blobName, APPLICATION_JSON_VALUE, blobMetadata)

    assertThat(blob.blobName).isEqualTo(blobName)
    assertThat(blob.data).isEqualTo(data)
    assertThat(blob.metadata).isEqualTo(blobMetadata)
    assertThat(blob.mimeType).isEqualTo(APPLICATION_JSON_VALUE)
  }

  @Test
  fun `save without filename in metadata`() {
    val blobName = randomUUID().toString()
    val blobMetadata = BlobMetadata.fromMap(emptyMap())
    val data = "{...}".toByteArray(UTF_8)
    val blob = quarantineRepository.save(data, blobName, APPLICATION_JSON_VALUE, blobMetadata)

    assertThat(blob.blobName).isEqualTo(blobName)
    assertThat(blob.data).isEqualTo(data)
    assertThat(blob.metadata).isEqualTo(blobMetadata)
    assertThat(blob.mimeType).isEqualTo(APPLICATION_JSON_VALUE)
  }

  @Test
  fun `save with trace header`() {
    val blobName = randomUUID().toString()
    val blobMetadata = BlobMetadata.fromMap()
    val data = "{...}".toByteArray(UTF_8)
    val blob = quarantineRepository.save(data, blobName, APPLICATION_JSON_VALUE, blobMetadata)

    val span = GlobalTracer.get().activeSpan()
    val traceId = span.context().toTraceId()
    val spanId = span.context().toSpanId()
    val parentSpanId = span.context().toSpanId()

    // construct trace header format: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
    val traceHeader = "$traceId-$spanId-$TRACE_SAMPLING_STATE_ACCEPT-$parentSpanId"

    assertThat(blob.metadata.get("trace_header")).isEqualTo(traceHeader)
  }

  @Test
  fun `generateSignedUploadUrl succeeds if quarantine storage is configured`() {
    val blobName = randomUUID().toString()
    assertDoesNotThrow { quarantineRepository.generateSignedUploadUrl(blobName) }
  }

  @Test
  fun `generateSignedUploadUrl throws when expiry is in past`() {
    val blobName = randomUUID().toString()
    assertThatIllegalArgumentException()
        .isThrownBy { quarantineRepository.generateSignedUploadUrl(blobName, -10) }
        .withMessageContaining("must be larger than 0")
  }

  @Test
  fun `generateSignedUploadUrl falls back to configuration when expiry is not set`() {
    val blobName = randomUUID().toString()
    val uploadUrl = quarantineRepository.generateSignedUploadUrl(blobName).uploadUrl

    val statusCode = upload(uploadUrl, "This is a test")
    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `generateSignedUploadUrl always returns a unique versionId`() {
    val blobName = randomUUID().toString()
    val newBlob1 = quarantineRepository.generateSignedUploadUrl(blobName)
    val newBlob2 = quarantineRepository.generateSignedUploadUrl(blobName)

    assertThat(newBlob1.versionId).isNotEqualTo(newBlob2.versionId)
  }

  @Test
  fun `generateSignedUploadUrl and upload to new blob succeeds`() {
    val blobName = randomUUID().toString()
    val uploadUrl = quarantineRepository.generateSignedUploadUrl(blobName).uploadUrl

    val statusCode = upload(uploadUrl, "hello world")
    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `generateSignedUploadUrl and overwriting existing blob succeeds`() {
    val blobName = randomUUID().toString()
    upload(quarantineRepository.generateSignedUploadUrl(blobName).uploadUrl, "hello world")

    val uploadUrl = quarantineRepository.generateSignedUploadUrl(blobName).uploadUrl

    val statusCode = upload(uploadUrl, "hello world 2")
    assertThat(statusCode).isEqualTo(201)
  }

  @Test
  fun `moveFromQuarantine throws if blob does not exist in quarantine`() {
    val blobName = randomUUID().toString()
    assertThatIllegalArgumentException()
        .isThrownBy {
          quarantineRepository.moveFromQuarantine(blobName, repository.blobContainerClient)
        }
        .withMessageContaining("does not exist")
  }

  @Test
  fun `moveFromQuarantine succeeds and removes quarantine prefix and version id suffix`() {
    deleteAllBlobs()

    val blobName = randomUUID().toString()
    val newBlob = quarantineRepository.generateSignedUploadUrl(blobName)
    upload(newBlob.uploadUrl, "hello world")
    repository.blobContainerClient.listBlobs().forEach { println(it.name) }
    assertThat(repository.blobContainerClient.listBlobs()).hasSize(0)

    val targetBlobName =
        quarantineRepository.moveFromQuarantine(
            newBlob.quarantineBlobName,
            repository.blobContainerClient,
            removeVersionIdSuffix = true,
            removeQuarantineDirectoryPrefix = true,
        )

    assertThat(targetBlobName).isEqualTo(blobName)
    val movedBlob = repository.find(blobName)!!
    assertThat(movedBlob.data).isEqualTo("hello world".toByteArray())
  }

  @Test
  fun `moveFromQuarantine succeeds and does not remove quarantine prefix and version id suffix`() {
    deleteAllBlobs()

    val blobName = randomUUID().toString()
    val newBlob = quarantineRepository.generateSignedUploadUrl(blobName)
    upload(newBlob.uploadUrl, "hello world")
    assertThat(repository.blobContainerClient.listBlobs()).hasSize(0)

    val targetBlobName =
        quarantineRepository.moveFromQuarantine(
            newBlob.quarantineBlobName,
            repository.blobContainerClient,
            removeVersionIdSuffix = false,
            removeQuarantineDirectoryPrefix = false,
        )

    assertThat(targetBlobName).isEqualTo(newBlob.quarantineBlobName)
    val movedBlob = repository.find(targetBlobName)!!
    assertThat(movedBlob.data).isEqualTo("hello world".toByteArray())
  }

  @Test
  fun `getQuarantineBlobClient fails if quarantineClient is null`() {
    val repo =
        QuarantineBlobStorageRepository(
            QuarantineBlobStorageProperties().apply { directory = "test" }, null)
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { repo.generateSignedUploadUrl(randomUUID().toString()) }
        .withMessage(
            "Quarantine StorageAccount not configured, is 'custom.quarantine-storage.connection-string' configured?")
  }

  private fun deleteAllBlobs() {
    repository.blobContainerClient.listBlobs()?.forEach { repository.deleteIfExists(it.name) }
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
