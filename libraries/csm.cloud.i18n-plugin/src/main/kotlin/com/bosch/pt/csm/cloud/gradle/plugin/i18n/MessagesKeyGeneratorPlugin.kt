/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.gradle.plugin.i18n

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile

open class MessagesKeyGeneratorPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    project.pluginManager.apply(JavaPlugin::class.java)
    val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
    val sourceSet = sourceSets.getByName("main") as SourceSet
    addMessageKeyTask(project, sourceSet)
  }

  private fun addMessageKeyTask(project: Project, sourceSet: SourceSet) {
    val taskName = sourceSet.getTaskName("generate", "messagesKeyKotlin")

    val task = project.tasks.create(taskName, MessagesKeyGenerator::class.java)
    task.group = GROUP_SOURCE_GENERATION

    val compileJavaTask = getCompileJavaTask(project, sourceSet)
    compileJavaTask.source(task.outputs)
  }

  companion object {
    private const val GROUP_SOURCE_GENERATION = "Source Generation"

    private fun getCompileJavaTask(project: Project, sourceSet: SourceSet): JavaCompile =
        project.tasks.getByName(sourceSet.compileJavaTaskName) as JavaCompile
  }
}
