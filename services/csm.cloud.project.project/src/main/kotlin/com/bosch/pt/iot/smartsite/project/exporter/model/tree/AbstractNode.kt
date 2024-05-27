/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import java.time.LocalDate
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import org.springframework.context.MessageSource

abstract class AbstractNode {

  abstract val identifier: UuidIdentifiable

  abstract val externalId: ExternalId?

  abstract val children: MutableList<AbstractNode>

  abstract val startDate: LocalDate?

  abstract val finishDate: LocalDate?

  abstract fun write(
      projectFile: ProjectFile,
      parent: Task?,
      messageSource: MessageSource
  ): List<ExternalId>

  protected fun addTask(projectFile: ProjectFile, parent: Task?): Task =
      if (parent == null) {
        projectFile.addTask()
      } else {
        parent.addTask()
      }
}
