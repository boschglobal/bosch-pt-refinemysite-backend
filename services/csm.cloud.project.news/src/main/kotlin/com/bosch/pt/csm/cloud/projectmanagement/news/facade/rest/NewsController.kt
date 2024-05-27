/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsListResource
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.factory.NewsListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
class NewsController(
    private val newsListResourceFactory: NewsListResourceFactory,
    private val newsService: NewsService,
) {

  @GetMapping(NEWS_BY_TASK_ID_ENDPOINT)
  fun findAllNewsForUserAndTask(
      @AuthenticationPrincipal user: User,
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: UUID
  ): NewsListResource {
    val newsList =
        newsService.findAllByUserIdentifierAndRootObject(
            user.identifier, ObjectIdentifier(TASK, taskId))

    return newsListResourceFactory.build(newsList)
  }

  @DeleteMapping(NEWS_BY_TASK_ID_ENDPOINT)
  fun deleteAllNewsForUserAndTask(
      @AuthenticationPrincipal user: User,
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: UUID
  ): ResponseEntity<String> {
    newsService.deleteAllByUserIdentifierAndRootObject(
        user.identifier, ObjectIdentifier(TASK, taskId))

    return ResponseEntity.noContent().build()
  }

  @DeleteMapping(NEWS_BY_PROJECT_ID_ENDPOINT)
  fun deleteAllNewsForUserAndProject(
      @AuthenticationPrincipal user: User,
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: UUID
  ): ResponseEntity<String> {
    newsService.deleteAllByUserIdentifierAndProjectIdentifier(user.identifier, projectId)

    return ResponseEntity.noContent().build()
  }

  @PostMapping(NEWS_SEARCH_ENDPOINT)
  fun findNewsForUserAndListOfTasks(
      @AuthenticationPrincipal user: User,
      @RequestBody taskIds: List<UUID>
  ): NewsListResource {
    val objectIdentifiers = taskIds.map { taskId: UUID -> ObjectIdentifier(TASK, taskId) }

    val newsList =
        newsService.findAllByUserIdentifierAndContextObjectsIn(user.identifier, objectIdentifiers)

    return newsListResourceFactory.build(newsList)
  }

  companion object {
    const val PATH_VARIABLE_TASK_ID = "taskId"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val NEWS_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/news"
    const val NEWS_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/news"
    const val NEWS_SEARCH_ENDPOINT = "/projects/tasks/news/search"
  }
}
