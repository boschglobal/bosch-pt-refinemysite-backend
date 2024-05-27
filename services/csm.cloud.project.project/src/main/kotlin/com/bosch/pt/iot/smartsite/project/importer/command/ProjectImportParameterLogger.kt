/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.command

import org.slf4j.LoggerFactory

object ProjectImportParameterLogger {

  private val LOGGER = LoggerFactory.getLogger(ProjectImportParameterLogger::class.java)

  fun log(
      blobName: String,
      readWorkingAreasHierarchically: Boolean?,
      craftColumn: String?,
      workAreaColumn: String?
  ) =
      LOGGER.info(
          "Run import job with parameters: " +
              "blobName=\"$blobName\", " +
              "hierarchical=\"$readWorkingAreasHierarchically\", " +
              "craftColumn=\"$craftColumn\", " +
              "workAreaColumn=\"$workAreaColumn\"")
}
