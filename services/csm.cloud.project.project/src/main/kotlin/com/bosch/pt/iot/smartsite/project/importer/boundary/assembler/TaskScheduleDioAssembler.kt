/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskScheduleDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskScheduleIdentifier
import net.sf.mpxj.ProjectFile
import org.springframework.stereotype.Component

@Component
class TaskScheduleDioAssembler : DioAssembler() {
  fun assemble(projectFile: ProjectFile, context: ImportContext): List<TaskScheduleDio> {
    val taskIdentifiersById =
        context.map.keys.filterIsInstance<TaskIdentifier>().associateBy { it.id }

    return projectFile.tasks
        .filter { taskIdentifiersById.contains(it.uniqueID) }
        .map {
          TaskScheduleDio(
              TaskScheduleIdentifier(it.uniqueID),
              it.guid,
              it.uniqueID,
              it.id,
              it.actualStart ?: it.plannedStart ?: it.start,
              it.actualFinish ?: it.plannedFinish ?: it.finish,
              requireNotNull(taskIdentifiersById[it.uniqueID] as TaskIdentifier),
          )
        }
        .filter { it.start != null || it.end != null }
  }
}
