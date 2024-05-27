package com.bosch.pt.iot.smartsite.importer.boundary.resource

import org.springframework.core.io.Resource

interface BlobResourceResolver {
  fun getBlobResource(filepath: String): Resource?
}
