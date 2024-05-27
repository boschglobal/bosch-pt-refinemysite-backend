/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio.Companion.PLACEHOLDER_CRAFT_NAME
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.CraftIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import java.util.UUID.randomUUID
import java.util.concurrent.atomic.AtomicInteger
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.TaskContainer
import org.springframework.stereotype.Component

@Component
class CraftDioAssembler : DioAssembler() {

  fun assemble(
      projectFile: ProjectFile,
      projectId: ProjectIdentifier,
      craftColumn: ImportColumn?,
      placeholderCraftCount: AtomicInteger
  ): List<CraftDio> {

    val tasks = projectFile.tasks
    val colorCount = AtomicInteger(1)

    return readCraftsFromTableColumn(
            craftColumn, tasks, projectId, colorCount, placeholderCraftCount)
        .sortedBy { it.name }
  }

  fun assemblePlaceholderCraft(
      craftColumn: ImportColumn?,
      tasks: TaskContainer,
      projectId: ProjectIdentifier,
      placeholderCraftCount: AtomicInteger
  ): CraftDio =
      craftColumn
          ?.let { column ->
            tasks.firstOrNull { ColumnUtils.getColumnValue(it, column) == PLACEHOLDER_CRAFT_NAME }
          }
          ?.let {
            // If the placeholder craft is already in the file, don't recreate it with a new id but
            // use the id of the existing one
            CraftDio(
                CraftIdentifier(it.uniqueID),
                it.guid,
                it.uniqueID,
                it.id,
                PLACEHOLDER_CRAFT_NAME,
                nextCraftColor(AtomicInteger(0)),
                projectId,
                placeholderCraftCount)
          }
          ?: CraftDio(
              CraftIdentifier(Int.MAX_VALUE),
              randomUUID(),
              Int.MAX_VALUE,
              Int.MAX_VALUE,
              PLACEHOLDER_CRAFT_NAME,
              nextCraftColor(AtomicInteger(0)),
              projectId,
              placeholderCraftCount)

  private fun readCraftsFromTableColumn(
      craftColumn: ImportColumn?,
      tasks: TaskContainer,
      projectId: ProjectIdentifier,
      colorCount: AtomicInteger,
      placeholderCraftCount: AtomicInteger
  ): Set<CraftDio> =
      craftColumn
          ?.let { column ->
            tasks
                .asSequence()
                .map {
                  val name = ColumnUtils.getColumnValue(it, column)
                  Pair(name?.trim(), it)
                }
                .filter { it.first != null }
                .toSet()
                .sortedBy { it.first }
                .map {
                  CraftDio(
                      CraftIdentifier(it.second.uniqueID),
                      it.second.guid,
                      it.second.uniqueID,
                      it.second.id,
                      it.first,
                      nextCraftColor(colorCount),
                      projectId,
                      placeholderCraftCount)
                }
                .toList()
          }
          // Filter out the placeholder craft - it is added separately
          ?.filter { it.name != PLACEHOLDER_CRAFT_NAME }
          ?.toSet()
          ?: setOf()

  companion object {

    private fun nextCraftColor(colorCount: AtomicInteger): String =
        CRAFT_COLORS[colorCount.getAndIncrement() % CRAFT_COLORS.size]

    private val CRAFT_COLORS =
        arrayOf(
            "#d9c200",
            "#f5a100",
            "#f87f0a",
            "#c96819",
            "#ff998e",
            "#ff5e45",
            "#b93516",
            "#ea0016",
            "#c384a1",
            "#9d4461",
            "#ca3fa1",
            "#ff60c5",
            "#b90276",
            "#bf00c1",
            "#8901a2",
            "#a05de7",
            "#7d38c7",
            "#50237f",
            "#7769ff",
            "#5641c3",
            "#779ef5",
            "#5275a1",
            "#005691",
            "#008ecf",
            "#0bd3d1",
            "#00a8b0",
            "#006249",
            "#2fb858",
            "#1e8a3e",
            "#78be20",
            "#859425",
            "#606a1b",
            "#b99363",
            "#896a43",
            "#ba7354",
            "#63372e",
            "#5e4b42",
            "#868686",
            "#525f6b",
            "#3d464f")
  }
}
