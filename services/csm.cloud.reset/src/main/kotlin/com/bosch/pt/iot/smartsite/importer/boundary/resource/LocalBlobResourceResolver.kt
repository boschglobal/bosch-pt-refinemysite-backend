package com.bosch.pt.iot.smartsite.importer.boundary.resource

import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
@Profile("!azure-blob-download")
class LocalBlobResourceResolver : BlobResourceResolver {

  override fun getBlobResource(filepath: String): Resource = ClassPathResource(filepath)
}
