package com.bosch.pt.iot.smartsite.importer.boundary.resource

import com.google.common.collect.Maps
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
object ClasspathResourceScanner {

  private const val FILENAME_PREFIX = "import-"
  private const val FILENAME_POSTFIX = ".json"

  fun scan(dataset: String): Map<ResourceTypeEnum, Resource> {
    return ResourceTypeEnum.values()
        .map { findImportFile(dataset, it) }
        .filter { it.value != null }
        .associate { it.key to it.value!! }
  }

  private fun findImportFile(
      dataset: String,
      type: ResourceTypeEnum
  ): Map.Entry<ResourceTypeEnum, Resource?> {
    val resource = ClassPathResource("$dataset/$FILENAME_PREFIX$type$FILENAME_POSTFIX")
    return Maps.immutableEntry<ResourceTypeEnum, Resource?>(
        type, if (resource.exists()) resource else null)
  }
}
