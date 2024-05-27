import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.blob.specialized.BlockBlobClient
import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import java.net.MalformedURLException
import java.net.URL
import java.time.OffsetDateTime

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

@ExcludeFromCodeCoverage
fun BlockBlobClient.generateSignedUrlWithPermissions(
    permissions: String,
    expiryInSeconds: Long,
): URL {
  require(this.blobName.isNotBlank()) { "blobName may not be empty" }
  val expiry = OffsetDateTime.now().plusSeconds(expiryInSeconds)
  val values = BlobServiceSasSignatureValues(expiry, BlobSasPermission.parse(permissions))
  val signature = this.generateSas(values)
  return try {
    URL("${this.blobUrl}?$signature")
  } catch (e: MalformedURLException) {
    error(e)
  }
}
