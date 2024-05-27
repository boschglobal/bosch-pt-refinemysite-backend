package com.bosch.pt.iot.smartsite.mpxj

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.PRIMAVERA_P6_XML
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.MPP
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.MSPDI
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.PMXML
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.function.Consumer
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.Task
import net.sf.mpxj.mspdi.MSPDIWriter
import net.sf.mpxj.primavera.PrimaveraPMFileWriter
import net.sf.mpxj.reader.UniversalProjectReader

open class MpxjIdBaseTest {

  companion object {
    val rootProjectGuid = "6dd9cc1f-a181-42b9-94b7-0d1ce63b6ae6".toUUID()
  }

  protected fun testIds(
      printXml: Boolean,
      format: ProjectExportFormatEnum,
      synchronizeTaskIDToHierarchy: Boolean,
      node: Node,
      expectedNode: ValidationNode
  ) {
    val output =
        write(format, synchronizeTaskIDToHierarchy) { projectFile ->
          // Insert a (invisible) root node for the project to MS Project files
          val optionalRootNode = addRootNodeIfNecessary(projectFile)
          addTasks(projectFile, optionalRootNode, null, node)
        }

    val projectFile = UniversalProjectReader().read(output.inputStream())
    val mppOrMSPDI = isMppOrMSPDI(projectFile)

    // Print
    if (printXml) println(String(output))
    println("Input:")
    printStructure(node, 2)
    println("\nActual:")
    printStructure(projectFile.tasks.first { if (mppOrMSPDI) it.id == 0 else it.id == 1 }, 2)
    println("\nExpected:")
    printStructure(expectedNode, 2)

    var index = 0

    if (mppOrMSPDI) {
      require(projectFile.tasks.size == 5) { "5 Tasks expected, found ${projectFile.tasks.size}" }

      val root = projectFile.tasks[0]
      require(root.id == 0) { "Root project id is: ${root.id}, expected: 0" }
      require(root.uniqueID == 0) { "Root project uniqueId is: ${root.uniqueID}, expected: 0" }
      require(root.guid == rootProjectGuid) {
        "Root project guid is: ${root.guid}, expected: $rootProjectGuid"
      }
      require(root.name == "Project1") { "Root project name is: ${root.name}, expected: Project1" }
      require(root.childTasks.size == 1) {
        "Root children size is: ${root.childTasks.size}, expected: 1"
      }

      // Start with index = 1
      index = 1
    } else {
      require(projectFile.tasks.size == 4) { "4 Tasks expected, found ${projectFile.tasks.size}" }
    }

    val parent = projectFile.tasks[index]
    validateNode(parent, expectedNode)
  }

  private fun printStructure(node: Node, indentation: Int) {
    println(
        "${" ".repeat(indentation)}${node.name} (id: ${node.id}, uniqueId: ${node.uniqueID}, guid: ${node.guid})")
    node.children.forEach { printStructure(it, indentation + 2) }
  }

  private fun printStructure(node: Task, indentation: Int) {
    println(
        "${" ".repeat(indentation)}${node.name} (id: ${node.id}, uniqueId: ${node.uniqueID}, guid: ${node.guid})")
    node.childTasks.forEach { printStructure(it, indentation + 2) }
  }

  private fun validateNode(task: Task, node: ValidationNode) {
    require(task.id == node.id) { "${task.name} id: ${task.id}, expected: ${node.id}" }
    require(task.uniqueID == node.uniqueID) {
      "${task.name} uniqueId: ${task.uniqueID}, expected: ${node.uniqueID}"
    }
    require(task.guid == node.guid) { "${task.name} guid: ${task.guid}, expected: ${node.guid}" }
    require(task.name == node.name) { "${task.name} name: should be: ${node.name}" }
    require(task.childTasks.size == node.children.size) {
      "${task.name} children: ${task.childTasks.size}, expected: ${node.children.size}"
    }

    node.children.forEachIndexed { idx, child ->
      val childTask = task.childTasks.firstOrNull { it.name == child.name }
      requireNotNull(childTask) { "${child.name} not found" }
      val collectionIndex = task.childTasks.indexOf(childTask)
      require(collectionIndex == idx) {
        "${child.name} was exported as the ${collectionIndex + 1}. child instead of the ${idx + 1}."
      }
      validateNode(childTask, child as ValidationNode)
    }
  }

  private fun addTasks(
      projectFile: ProjectFile,
      optionalRootNode: Task?,
      parentTask: Task?,
      node: Node
  ) {
    projectFile
        .insert(optionalRootNode, parentTask)
        .also {
          it.name = node.name
          it.id = node.id
          it.uniqueID = node.uniqueID
          it.guid = node.guid
        }
        .also { currentNodeTask ->
          val lazyNodes = mutableListOf<AfterDirectInsertNode>()
          node.children.forEach { child ->
            if (child is AfterDirectInsertNode) {
              // Don't add the child immediately, apply eager nodes first
              lazyNodes.add(child)
            } else {
              addTasks(projectFile, optionalRootNode, currentNodeTask, child)
            }
          }
          lazyNodes.forEach { child ->
            addTasks(projectFile, optionalRootNode, currentNodeTask, child)
          }
        }
  }

  private fun write(
      format: ProjectExportFormatEnum,
      synchronizeTaskIDToHierarchy: Boolean,
      consumer: Consumer<ProjectFile>
  ): ByteArray {
    val projectFile = projectFile(format)

    consumer.accept(projectFile)

    if (synchronizeTaskIDToHierarchy) projectFile.tasks.synchronizeTaskIDToHierarchy()
    return ByteArrayOutputStream()
        .apply { getProjectWriter(format).write(projectFile, this) }
        .toByteArray()
  }

  private fun addRootNodeIfNecessary(projectFile: ProjectFile): Task? =
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
          guid = rootProjectGuid
        }
      } else null

  private fun isMppOrMSPDI(projectFile: ProjectFile) =
      projectFile.projectProperties.fileType == MPP.name ||
          projectFile.projectProperties.fileType == MSPDI.name

  private fun projectFile(format: ProjectExportFormatEnum) =
      ProjectFile().apply {
        projectProperties.projectTitle = "Project1"
        projectProperties.projectID = "Project1"
        projectProperties.fileType =
            when (format) {
              MS_PROJECT_XML -> MSPDI.name
              PRIMAVERA_P6_XML -> PMXML.name
            }
      }

  private fun ProjectFile.insert(
      optionalRootNode: Task?,
      parent: Task?,
  ): Task = (parent ?: optionalRootNode)?.addTask() ?: this.tasks.add()

  private fun getProjectWriter(format: ProjectExportFormatEnum) =
      when (format) {
        MS_PROJECT_XML -> MSPDIWriter()
        PRIMAVERA_P6_XML -> PrimaveraPMFileWriter()
      }

  @Suppress("UnnecessaryAbstractClass")
  protected abstract class Node(
      val name: String,
      val id: Int,
      val uniqueID: Int,
      val guid: UUID,
      val children: List<Node>
  )

  /**
   * A node to add to the xml who's children are inserted immediately. Children can be of type
   * [DirectInsertNode] or [AfterDirectInsertNode]. Children of type [DirectInsertNode] are directly
   * inserted (recursive). Children of type [AfterDirectInsertNode] are inserted after all Children
   * of type [DirectInsertNode] of this node.
   */
  protected class DirectInsertNode(
      name: String,
      id: Int,
      uniqueID: Int,
      guid: UUID,
      children: List<Node>
  ) : Node(name, id, uniqueID, guid, children)

  /**
   * A node to add to the xml who's children are inserted after all Children of type
   * [DirectInsertNode]. Children can be of type [DirectInsertNode] or [AfterDirectInsertNode]. This
   * node type has the purpose of influencing the insert order (what is required to provoke special
   * ID orders).
   */
  protected class AfterDirectInsertNode(
      name: String,
      id: Int,
      uniqueID: Int,
      guid: UUID,
      children: List<Node>
  ) : Node(name, id, uniqueID, guid, children)

  /** A node that can have children of its own type. It is evaluated immediately recursive. */
  protected class ValidationNode(
      name: String,
      id: Int,
      uniqueID: Int,
      guid: UUID,
      children: List<ValidationNode>
  ) : Node(name, id, uniqueID, guid, children)
}
