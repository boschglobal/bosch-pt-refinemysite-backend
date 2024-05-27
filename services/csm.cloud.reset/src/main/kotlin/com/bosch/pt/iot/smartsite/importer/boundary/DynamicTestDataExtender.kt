/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.importer.boundary

import com.bosch.pt.iot.smartsite.dataimport.project.model.Milestone
import com.bosch.pt.iot.smartsite.dataimport.project.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.dataimport.project.model.Project
import com.bosch.pt.iot.smartsite.dataimport.project.model.WorkArea
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCard
import com.bosch.pt.iot.smartsite.dataimport.task.model.Task
import com.bosch.pt.iot.smartsite.dataimport.task.model.Topic
import com.bosch.pt.iot.smartsite.importer.boundary.AbstractDataImportService.Companion.getResources
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ClasspathResourceScanner
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.daycard
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.milestone
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.project
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.task
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.topic
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.workarea
import com.fasterxml.jackson.core.type.TypeReference
import java.util.UUID.randomUUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import org.springframework.core.io.Resource

object DynamicTestDataExtender {

  fun generateData(dataset: String, numberOfAdditionalWorkAreas: Int): GeneratedData {

    val resources: Map<ResourceTypeEnum, Resource> = ClasspathResourceScanner.scan(dataset)
    if (resources.isEmpty()) {
      throw IllegalStateException("The specified dataset does not contain any files")
    }

    // Load existing data from json files

    val existingWorkAreas =
        getResources(resources, workarea, object : TypeReference<List<WorkArea>>() {})

    val existingTasks = getResources(resources, task, object : TypeReference<List<Task>>() {})

    val existingTopics = getResources(resources, topic, object : TypeReference<List<Topic>>() {})

    val existingDayCards =
        getResources(resources, daycard, object : TypeReference<List<DayCard>>() {})

    val existingMilestones =
        getResources(resources, milestone, object : TypeReference<List<Milestone>>() {})

    // Copy lists to be extended

    val newWorkAreas = existingWorkAreas.toMutableList()
    val newTasks = existingTasks.toMutableList()
    val newTopics = existingTopics.toMutableList()
    val newDayCards = existingDayCards.toMutableList()
    val newMilestones = existingMilestones.toMutableList()

    // Generate data for each project

    val projects = getResources(resources, project, object : TypeReference<List<Project>>() {})
    projects.forEach { project ->

      // Find the work area with the most tasks to take ist as a template to duplicate
      val workAreaIdWithMostTasks =
          existingTasks
              .filter { it.projectId == project.id && it.workAreaId != null }
              .groupingBy { it.workAreaId }
              .eachCount()
              .maxWithOrNull(Comparator.comparingInt { it.value })
              ?.key

      if (workAreaIdWithMostTasks != null) {

        // Find data to duplicate
        val templateWorkArea = existingWorkAreas.single { it.id == workAreaIdWithMostTasks }
        val templateTasks = existingTasks.filter { it.workAreaId == workAreaIdWithMostTasks }

        var dateOffset = 0
        var workAreaListEtag =
            existingWorkAreas.filter { it.projectId == project.id }.maxOf { it.etag.toInt() } + 1

        // Duplicate the work area and data belonging it
        for (workAreaIndex in 1..numberOfAdditionalWorkAreas) {

          // Duplicate work area
          val duplicatedWorkArea =
              duplicateWorkArea(templateWorkArea, workAreaIndex, workAreaListEtag++).also {
                newWorkAreas.add(it)
              }

          // Duplicate tasks of work area
          val oldTaskIdsToNewTaskIdsMapping =
              copyTasks(duplicatedWorkArea.id, templateTasks, dateOffset).let {
                newTasks.addAll(it.first)
                it.second
              }

          // Duplicate topics and day cards

          fun isCopiedTask(id: String) = oldTaskIdsToNewTaskIdsMapping.keys.contains(id)

          copyTopics(
              existingTopics.filter { isCopiedTask(it.taskId) }, oldTaskIdsToNewTaskIdsMapping)
              .also { newTopics.addAll(it) }

          copyDayCards(
              existingDayCards.filter { isCopiedTask(it.taskId) }, oldTaskIdsToNewTaskIdsMapping)
              .also { newDayCards.addAll(it) }

          // Add the next work area where tasks start with a delay of 2 days (compared to the
          // previous copy of the work area)
          dateOffset += 2
        }

        // Generate additional day cards to have more tasks with day cards
        generateDayCards(newTasks, newDayCards)
      }

      newMilestones.addAll(generateWorkAreaMilestones(newWorkAreas, newTasks, project))
      newMilestones.addAll(generateProjectMilestones(newTasks, project))
    }

    return GeneratedData(newWorkAreas, newTasks, newTopics, newDayCards, newMilestones)
  }

  private fun generateDayCards(tasks: List<Task>, dayCards: MutableList<DayCard>) {
    val dayCardsByTaskId = dayCards.groupBy { it.taskId }
    tasks.forEach { task ->

      // Add day cards if a task has a schedule with a start and end date
      if (task.end != null && task.start != null) {
        val taskDuration = (task.end - task.start) + 1
        require(taskDuration >= 0) { "Negative task duration" }

        val dayCardsOfTask = dayCardsByTaskId[task.id]
        val daysWithDayCards = dayCardsOfTask?.count() ?: 0
        val currentDayCardPercentage = daysWithDayCards / taskDuration.toFloat()

        // Do it only if the task's schedule has less than 20% of its timespan filled with day
        // cards. Do it only in 80% if the cases.
        if (currentDayCardPercentage < 0.2 && ThreadLocalRandom.current().nextFloat() > 0.2) {

          // Calculate numbers/metrics for the day cards to create
          val expectedMinimumOfDayCards = ceil(taskDuration * 0.2).toInt()
          val missingDayCards = expectedMinimumOfDayCards - daysWithDayCards

          // Generate dates where in the schedule the new day cards should be added
          val additionalDayCardDays =
              (1..missingDayCards)
                  .map { ThreadLocalRandom.current().nextInt(0, taskDuration) }
                  .toMutableSet()

          val existingDays = dayCardsOfTask?.map { it.date }?.toSet() ?: emptySet()
          additionalDayCardDays.removeAll(existingDays)

          // Generate new day cards based on the previously calculated parameters
          val newDayCards =
              additionalDayCardDays.sorted().map {
                require(task.start == task.end && it == 0 || task.start + it <= task.end) {
                  "Day card date outside of schedule"
                }
                DayCard(
                    id = randomUUID().toString(),
                    taskId = task.id,
                    title = task.name,
                    manpower = ThreadLocalRandom.current().nextInt(1, 6).toBigDecimal(),
                    date = it,
                    createWithUserId = task.createWithUserId)
              }

          dayCards.addAll(newDayCards)
        }
      }
    }
  }

  private fun generateWorkAreaMilestones(
      workAreas: List<WorkArea>,
      tasks: List<Task>,
      project: Project
  ): List<Milestone> =
      tasks
          .asSequence()
          .filter { it.projectId == project.id && it.workAreaId != null }
          .filter { it.start != null && it.end != null }
          .groupBy { requireNotNull(it.workAreaId) }
          .filter { it.value.isNotEmpty() }
          .map { (workAreaId, tasksOfWorkArea) ->

            // Create a milestone per work area
            val workArea = workAreas.single { it.id == workAreaId }

            // Calculate the date of the milestone
            val latestEnd = tasksOfWorkArea.maxOf { it.end!! }

            // Put the milestone randomly in a 10 days date range after the last
            // task of the work area
            val date = latestEnd + ThreadLocalRandom.current().nextInt(0, 10)

            Milestone(
                id = randomUUID().toString(),
                projectId = project.id,
                name = workArea.name,
                type = PROJECT,
                header = false,
                date = date,
                workAreaId = workAreaId,
                createWithUserId = workArea.createWithUserId)
          }

  private fun generateProjectMilestones(tasks: List<Task>, project: Project): List<Milestone> {

    // Generate project milestones in 5% steps to indicate project progress
    val latestEnd = tasks.maxOf { it.end ?: 0 }
    val earliestStart = tasks.minOf { it.start ?: 0 }

    val projectDuration = latestEnd - earliestStart

    // If the project is shorter than 20 days don't create the milestones
    if (projectDuration < 20) {
      return emptyList()
    }

    // Create the milestones
    fun createProjectMilestone(name: String, date: Int) =
        Milestone(
            id = randomUUID().toString(),
            projectId = project.id,
            name = name,
            type = PROJECT,
            header = true,
            date = date,
            description = "$name progress milestone",
            createWithUserId = requireNotNull(project.createWithUserId))

    return (5..100 step 5).map { progressInPercent ->
      val date = earliestStart + progressInPercent * (projectDuration / 100.0)
      createProjectMilestone("$progressInPercent%", date.toInt())
    }
  }

  private fun copyDayCards(
      existingDayCards: List<DayCard>,
      oldTaskIdsToNewTaskIdsMapping: Map<String, String>
  ): List<DayCard> =
      existingDayCards.map {
        it.copy(
            id = randomUUID().toString(),
            taskId = requireNotNull(oldTaskIdsToNewTaskIdsMapping[it.taskId]))
      }

  private fun copyTopics(
      existingTopics: List<Topic>,
      oldTaskIdsToNewTaskIdsMapping: Map<String, String>
  ): List<Topic> =
      existingTopics.map {
        it.copy(
            id = randomUUID().toString(),
            taskId = requireNotNull(oldTaskIdsToNewTaskIdsMapping[it.taskId]))
      }

  private fun copyTasks(
      targetWorkAreaId: String,
      existingTasks: List<Task>,
      dateOffset: Int
  ): Pair<List<Task>, Map<String, String>> {
    val oldTaskIdsToNewTaskIdsMapping = mutableMapOf<String, String>()
    val copiedTasks =
        existingTasks.map { task ->
          val newTaskId = randomUUID().toString()

          oldTaskIdsToNewTaskIdsMapping[task.id] = newTaskId
          task.copy(
              id = newTaskId,
              workAreaId = targetWorkAreaId,
              start = task.start?.let { it + dateOffset },
              end = task.end?.let { it + dateOffset })
        }
    return Pair(copiedTasks, oldTaskIdsToNewTaskIdsMapping)
  }

  private fun duplicateWorkArea(workArea: WorkArea, number: Int, etag: Int): WorkArea =
      workArea.copy(
          id = randomUUID().toString(),
          name = "Additional Work Area $number",
          etag = "$etag",
      )
}

class GeneratedData(
    val workAreas: List<WorkArea>,
    val tasks: List<Task>,
    val topics: List<Topic>,
    val dayCards: List<DayCard>,
    val milestones: List<Milestone>
)
