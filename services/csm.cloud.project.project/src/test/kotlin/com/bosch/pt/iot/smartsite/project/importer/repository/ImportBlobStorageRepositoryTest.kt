/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.repository

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.blob.model.MalwareScanResult.NOT_SCANNED
import com.bosch.pt.iot.smartsite.application.config.QuarantineBlobStorageProperties
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository.Companion.MALWARE_SCANNING_RESULT_KEY
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.USER
import com.bosch.pt.iot.smartsite.user.model.User
import com.azure.core.util.polling.LongRunningOperationStatus.FAILED
import com.azure.core.util.polling.LongRunningOperationStatus.SUCCESSFULLY_COMPLETED
import com.azure.core.util.polling.PollResponse
import com.azure.core.util.polling.SyncPoller
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobCopyInfo
import com.azure.storage.blob.models.BlobProperties
import com.azure.storage.blob.specialized.BlobInputStream
import com.azure.storage.blob.specialized.BlockBlobClient
import generateSignedUrlWithPermissions
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.test.context.TestSecurityContextHolder.setAuthentication

@SmartSiteMockKTest
class ImportBlobStorageRepositoryTest {

  @MockK private lateinit var blobContainerClient: BlobContainerClient

  @MockK private lateinit var quarantineContainerClient: BlobContainerClient

  @MockK private lateinit var environment: Environment

  @RelaxedMockK private lateinit var quarantineProperties: QuarantineBlobStorageProperties

  @SpyK @InjectMockKs private lateinit var repository: ImportBlobStorageRepository

  @AfterEach
  fun cleanUp() {
    clearMocks(blobContainerClient, quarantineContainerClient, quarantineProperties, environment)
  }

  @Test
  fun `save blob with name`() {
    val blobName = "file"
    val data = "data".toByteArray()

    every { quarantineContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { getBlockBlobClient() } returns mockk<BlockBlobClient>(relaxed = true)
        }

    val blob = repository.save(blobName, data, "file-name", TEXT_PLAIN_VALUE, mutableMapOf())
    assertThat(blob.blobName).isEqualTo(blobName)
    assertThat(blob.metadata.get("filename")).isEqualTo("file_name")
    assertThat(blob.mimeType).isEqualTo(TEXT_PLAIN_VALUE)
  }

  @Test
  fun `save throws IO Exception`() {
    mockk<User>()
        .apply {
          every { authorities } returns createAuthorityList(USER.roleName())
          every { identifier } returns randomUUID()
        }
        .also { setAuthentication(UsernamePasswordAuthenticationToken(it, null, it.authorities)) }

    val expectedErrorMessage = "Expected error"
    every { quarantineContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { blockBlobClient } returns
              mockk<BlockBlobClient>().apply {
                every {
                  uploadWithResponse(any(), any(), any(), any(), any(), any(), any(), any(), any())
                } throws IOException(expectedErrorMessage)
              }
        }

    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy {
          repository.save("data".toByteArray(), "file-name", TEXT_PLAIN_VALUE, mutableMapOf())
        }
        .withMessageContaining(expectedErrorMessage)
  }

  @Test
  fun `find returns requested blob`() {
    val blobName = "file"
    val data = "data".toByteArray()

    val blobClient =
        mockk<BlobClient>().apply {
          every { exists() } returns true
          every { downloadStream(any()) } answers
              {
                (it.invocation.args[0] as ByteArrayOutputStream).writeBytes(data)
              }
          every { properties } returns
              mockk<BlobProperties>().apply {
                every { contentType } returns TEXT_PLAIN_VALUE
                every { metadata } returns mutableMapOf("trace_header" to "--1-")
              }
        }

    every { blobContainerClient.getBlobClient(any()) } returns blobClient

    val blob = repository.find(blobName)!!
    assertThat(blob.blobName).isEqualTo(blobName)
    assertThat(blob.data).isEqualTo(data)
    assertThat(blob.mimeType).isEqualTo(TEXT_PLAIN_VALUE)
    assertThat(blob.metadata.toMap()).isEqualTo(mapOf("trace_header" to "--1-"))
  }

  @Test
  fun `find blob returns null if the blob does not exist`() {
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply { every { exists() } returns false }

    assertThat(repository.find(randomUUID().toString())).isNull()
  }

  @Test
  fun `find blob throws io exception`() {
    every { blobContainerClient.getBlobClient(any()) } throws IOException("error")

    assertThat(repository.find(randomUUID().toString())).isNull()
  }

  @Test
  fun `get malware scan result`() {
    val blobClient =
        mockk<BlobClient>().apply {
          every { tags } returns mapOf(MALWARE_SCANNING_RESULT_KEY to NOT_SCANNED.text)
        }

    every { quarantineContainerClient.getBlobClient(any()) } returns blobClient
    every { environment.acceptsProfiles(any<Profiles>()) } returns false

    assertThat(repository.getMalwareScanResult("file").name).isEqualTo(NOT_SCANNED.name)
  }

  @Test
  fun `get malware scan result blocking with retries`() {
    val blobClient =
        mockk<BlobClient>().apply {
          every { tags } returns mapOf(MALWARE_SCANNING_RESULT_KEY to NOT_SCANNED.text)
        }

    every { quarantineContainerClient.getBlobClient(any()) } returns blobClient
    every { environment.acceptsProfiles(any<Profiles>()) } returns false
    every { repository.delayMalwareScanResultCheck() } answers {}

    assertThat(repository.getMalwareScanResultBlocking("file").name).isEqualTo(NOT_SCANNED.name)

    verify(exactly = 31) { blobClient.tags }
  }

  @Test
  fun `read returns blob content as stream`() {
    val inputStream = mockk<BlobInputStream>()
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { blockBlobClient } returns
              mockk<BlockBlobClient>().apply {
                every { exists() } returns true
                every { openInputStream() } returns inputStream
              }
        }

    assertThat(repository.read("directory/blob")).isEqualTo(inputStream)
  }

  @Test
  fun `read returns null if blob does not exist`() {
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { blockBlobClient } returns
              mockk<BlockBlobClient>().apply { every { exists() } returns false }
        }

    assertThat(repository.read("non-existent/blob")).isNull()
  }

  @Test
  fun `deleteIfExists deletes existing blob`() {
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { blockBlobClient } returns
              mockk<BlockBlobClient>(relaxed = true).apply { every { exists() } returns true }
        }

    assertThat(repository.deleteIfExists("directory/blob")).isTrue()
  }

  @Test
  fun `deleteIfExists ignores blob not found`() {
    val blobName = "not-exists"
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { blockBlobClient } returns
              mockk<BlockBlobClient>().apply {
                every { getBlobName() } returns blobName
                every { exists() } returns false
              }
        }

    assertThat(repository.deleteIfExists(blobName)).isFalse()
  }

  @Test
  fun `deleteIfExists ignores non-existing blob`() {
    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply { every { blockBlobClient } returns null }

    assertThat(repository.deleteIfExists("not-exists")).isFalse()
  }

  @Test
  fun `move from quarantine`() {
    val quarantineBlockBlobClient =
        mockk<BlockBlobClient>().apply {
          every { exists() } returns true
          every { deleteIfExists() } returns true
        }

    // Mock extension function
    mockkStatic(quarantineBlockBlobClient::generateSignedUrlWithPermissions)
    every { any<BlockBlobClient>().generateSignedUrlWithPermissions(any(), any()) } returns
        URL("https://google.com")

    every { quarantineContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { getBlockBlobClient() } returns quarantineBlockBlobClient
        }

    val syncPoller =
        mockk<SyncPoller<BlobCopyInfo, Void>>().apply {
          every { waitForCompletion() } returns
              mockk<PollResponse<BlobCopyInfo>>().apply {
                every { status } returns SUCCESSFULLY_COMPLETED
              }
        }

    val targetBlobBlobClient =
        mockk<BlockBlobClient>().apply { every { beginCopy(any(), any()) } returns syncPoller }

    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply { every { blockBlobClient } returns targetBlobBlobClient }

    repository.moveFromQuarantine("blob")

    verifyOrder {
      quarantineBlockBlobClient.exists()
      quarantineBlockBlobClient.generateSignedUrlWithPermissions(any(), any())
      targetBlobBlobClient.beginCopy(any(), any())
      syncPoller.waitForCompletion()
      quarantineBlockBlobClient.deleteIfExists()
    }
  }

  @Test
  fun `move from quarantine fails`() {
    val quarantineBlockBlobClient =
        mockk<BlockBlobClient>().apply {
          every { exists() } returns true
          every { deleteIfExists() } returns true
        }

    // Mock extension function
    mockkStatic(quarantineBlockBlobClient::generateSignedUrlWithPermissions)
    every { any<BlockBlobClient>().generateSignedUrlWithPermissions(any(), any()) } returns
        URL("https://google.com")

    every { quarantineContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply {
          every { getBlockBlobClient() } returns quarantineBlockBlobClient
        }

    val syncPoller =
        mockk<SyncPoller<BlobCopyInfo, Void>>().apply {
          every { waitForCompletion() } returns
              mockk<PollResponse<BlobCopyInfo>>().apply {
                every { status } returns FAILED
                every { value } returns mockk(relaxed = true)
              }
        }

    val targetBlobBlobClient =
        mockk<BlockBlobClient>().apply { every { beginCopy(any(), any()) } returns syncPoller }

    every { blobContainerClient.getBlobClient(any()) } returns
        mockk<BlobClient>().apply { every { blockBlobClient } returns targetBlobBlobClient }

    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { repository.moveFromQuarantine("blob") }
        .withMessageContaining("Could not copy from quarantine, status:")

    verifyOrder {
      quarantineBlockBlobClient.exists()
      quarantineBlockBlobClient.generateSignedUrlWithPermissions(any(), any())
      targetBlobBlobClient.beginCopy(any(), any())
      syncPoller.waitForCompletion()
    }

    verify(exactly = 0) { quarantineBlockBlobClient.deleteIfExists() }
  }
}
