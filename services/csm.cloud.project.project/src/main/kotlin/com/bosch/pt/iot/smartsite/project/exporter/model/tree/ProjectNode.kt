/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.ProjectUtils.isMppOrMSPDI
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.time.LocalDate
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import org.springframework.context.MessageSource

class ProjectNode(
    override val identifier: ProjectId,
    val project: Project,
    override val children: MutableList<AbstractNode> = mutableListOf()
) : AbstractNode() {

  override val externalId: ExternalId? = null
  override val startDate: LocalDate = project.start
  override val finishDate: LocalDate = project.end

  override fun write(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): List<ExternalId> {
    check(parent == null) { "Parent of root must be null" }

    val rootTask = addRootTaskIfExportForMsProject(projectFile)

    return children
        .sortedBy { it.externalId?.fileId }
        .map { it.write(projectFile, rootTask, messageSource) }
        .flatten()
  }

  private fun addRootTaskIfExportForMsProject(projectFile: ProjectFile): Task? =
      if (isMppOrMSPDI(projectFile)) {
        projectFile.addTask().apply {
          name = projectFile.projectProperties.projectTitle
          // Set to hide the task
          uniqueID = 0
          id = 0
          // The other settings are optional but make it more consistent
          // to xml exports from ms project
          wbs = "0"
          outlineLevel = 0
          outlineNumber = "0"
          guid = this@ProjectNode.project.identifier.identifier
        }
      } else null

  override fun toString(): String = "ProjectNode(identifier=$identifier," + "name=${project.title})"
}
