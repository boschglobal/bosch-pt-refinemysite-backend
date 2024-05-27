/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.ParticipantPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.UserPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.assembler.UserPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.UserQueryService
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class UserGraphQlController(
    private val userPayloadAssembler: UserPayloadAssembler,
    private val userQueryService: UserQueryService
) {

  @BatchMapping
  fun user(participants: List<ParticipantPayloadV1>): Map<ParticipantPayloadV1, UserPayloadV1?> {
    val requestedUserIds = participants.map { it.userId }
    val users =
        userQueryService
            .findAllByIdentifiers(requestedUserIds)
            .associateBy { it.identifier }
            .toMutableMap()

    val deletedUsers = requestedUserIds - users.map { it.key }.toSet()
    val deletedUserProjections =
        deletedUsers.associateWith {
          UserProjection(
              UserId(randomUUID()),
              0L,
              "<empty>",
              "Deleted",
              "User",
              "donotreply@bosch-refinemysite.com",
              null,
              null,
              admin = false,
              locked = false,
              locale = null,
              country = null,
              crafts = emptyList(),
              phoneNumbers = emptyList(),
              eventAuthor = UserId(randomUUID()),
              eventDate = LocalDateTime.MIN,
              history = emptyList())
        }

    // Add list of fake user projections for deleted users to the map
    users.putAll(deletedUserProjections)

    return participants.associateWith {
      users[it.userId]?.let { userPayloadAssembler.assemble(it) }
    }
  }
}
