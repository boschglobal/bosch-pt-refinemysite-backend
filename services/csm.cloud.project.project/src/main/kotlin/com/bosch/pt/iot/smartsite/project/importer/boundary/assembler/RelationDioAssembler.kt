/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.dio.RelationDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.MilestoneIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.RelationIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import java.util.UUID.randomUUID
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.RelationType
import org.springframework.stereotype.Component

@Component
class RelationDioAssembler : DioAssembler() {

  fun assemble(projectFile: ProjectFile, context: ImportContext): List<RelationDio> {
    // Relations don't have a unique ID therefore we introduce a custom number
    // The ImportRelationIdentifier isn't used anywhere, therefore it doesn't
    // have any consequences.
    var index = 0
    return projectFile.tasks
        .filter { it.successors.isNotEmpty() }
        .mapNotNull { task ->
          task.successors
              .filter { it.type == RelationType.FINISH_START }
              .mapNotNull relation_label@{ relation ->
                val source =
                    context.get(
                        relation.sourceTask.uniqueID,
                        MilestoneIdentifier::class.java,
                        TaskIdentifier::class.java)
                val target =
                    context.get(
                        relation.targetTask.uniqueID,
                        MilestoneIdentifier::class.java,
                        TaskIdentifier::class.java)

                // Skip all relations that are not of type task-task, task-milestone,
                // milestone-task or milestone-milestone
                if (source == null || target == null) {
                  return@relation_label null
                }

                RelationDio(
                    RelationIdentifier(index++),
                    randomUUID(),
                    Int.MAX_VALUE,
                    Int.MAX_VALUE,
                    source,
                    target)
              }
        }
        .flatten()
        .distinctBy { Pair(it.sourceId, it.targetId) }
  }
}
