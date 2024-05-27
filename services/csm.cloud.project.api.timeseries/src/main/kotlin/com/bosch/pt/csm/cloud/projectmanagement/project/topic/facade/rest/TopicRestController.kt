/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.assembler.TopicListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.service.TopicQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TopicRestController(
    private val topicListResourceAssembler: TopicListResourceAssembler,
    private val participantQueryService: ParticipantQueryService,
    private val topicQueryService: TopicQueryService
) {

  companion object {
    const val TOPICS_ENDPOINT = "/projects/tasks/topics"
  }

  @GetMapping(TOPICS_ENDPOINT)
  fun findTopics(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<TopicListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val topics =
        when (latestOnly) {
          true -> topicQueryService.findTopicsOfProjectsAndDeletedFalse(projectIds)
          else -> topicQueryService.findTopicsOfProjects(projectIds)
        }
    return ResponseEntity.ok().body(topicListResourceAssembler.assemble(topics, latestOnly))
  }
}
