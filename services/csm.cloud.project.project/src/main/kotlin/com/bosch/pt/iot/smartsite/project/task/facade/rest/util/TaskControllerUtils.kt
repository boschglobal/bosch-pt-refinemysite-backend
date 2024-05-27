/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.util

object TaskControllerUtils {

  const val PATH_VARIABLE_TASK_ID = "taskId"
  const val PATH_VARIABLE_PROJECT_ID = "projectId"

  const val TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}"
  const val TASKS_BATCH_ENDPOINT = "/projects/tasks/batch"
  const val TASKS_ENDPOINT = "/projects/tasks"
  const val TASKS_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/tasks"
  const val TASKS_SEARCH_ENDPOINT = "/projects/{projectId}/tasks/search"
  const val ASSIGN_TASKS_BATCH_ENDPOINT = "/projects/tasks/assign"
  const val ASSIGN_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/assign"
  const val UNASSIGN_TASKS_BATCH_ENDPOINT = "/projects/tasks/unassign"
  const val UNASSIGN_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/unassign"
  const val CLOSE_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/close"
  const val CLOSE_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/close"
  const val SEND_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/send"
  const val SEND_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/send"
  const val START_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/start"
  const val START_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/start"
  const val RESET_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/reset"
  const val RESET_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/reset"
  const val ACCEPT_TASK_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/accept"
  const val ACCEPT_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/accept"

  const val DELETE_TASKS_BATCH_ENDPOINT = "/projects/{projectId}/tasks/delete"
  const val FIND_BATCH_ENDPOINT = "/projects/{projectId}/tasks/batch/find"
}
