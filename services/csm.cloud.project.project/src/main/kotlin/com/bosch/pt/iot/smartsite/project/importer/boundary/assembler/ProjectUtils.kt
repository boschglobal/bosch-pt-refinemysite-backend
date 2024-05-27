/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.MPP
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.MSPDI
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.PMXML
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.PP
import net.sf.mpxj.ProjectFile

object ProjectUtils {

  /**
   * P6 xml file format is different to the other formats, as it doesn't have the project itself as
   * a task root-node. Therefore, a few extra checks must be added to the code to handle the P6 xml
   * files correctly.
   */
  fun isP6Xml(projectFile: ProjectFile) =
      projectFile.projectProperties.fileType == PMXML.name ||
          projectFile.projectProperties.fileType == PP.name

  fun isMppOrMSPDI(projectFile: ProjectFile) =
      projectFile.projectProperties.fileType == MPP.name ||
          projectFile.projectProperties.fileType == MSPDI.name
}
