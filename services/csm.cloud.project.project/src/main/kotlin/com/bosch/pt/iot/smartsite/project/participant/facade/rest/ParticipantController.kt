/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignParticipantAsAdminCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.RemoveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.ResendInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.UpdateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.AssignParticipantAsAdminCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.handler.AssignParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.handler.RemoveParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.handler.ResendInvitationCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.handler.UpdateParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.AssignParticipantAsAdminResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.AssignParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.SearchParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request.UpdateParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantCompanyReferenceListResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory.ParticipantCompanyListResourceFactory
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory.ParticipantListResourceFactory
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory.ParticipantResourceFactory
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.STATUS
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.USER_FIRST_NAME
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant.Companion.USER_LAST_NAME
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.annotation.Nonnull
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.created
import org.springframework.http.ResponseEntity.noContent
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath

@ApiVersion
@RestController
open class ParticipantController(
    private val participantListResourceFactory: ParticipantListResourceFactory,
    private val participantResourceFactory: ParticipantResourceFactory,
    private val participantCompanyListResourceFactory: ParticipantCompanyListResourceFactory,
    private val participantQueryService: ParticipantQueryService,
    private val updateParticipantCommandHandler: UpdateParticipantCommandHandler,
    private val resendInvitationCommandHandler: ResendInvitationCommandHandler,
    private val assignParticipantAsAdminCommandHandler: AssignParticipantAsAdminCommandHandler,
    private val assignParticipantCommandHandler: AssignParticipantCommandHandler,
    private val removeParticipantCommandHandler: RemoveParticipantCommandHandler
) {

  @PostMapping(PARTICIPANTS_BY_PROJECT_SEARCH_ENDPOINT)
  open fun findAllParticipantsWithFilters(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody body: SearchParticipantResource,
      @PageableDefault(
          sort = [STATUS, USER_LAST_NAME, USER_FIRST_NAME],
          direction = ASC,
          size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<ParticipantListResource> =
      participantQueryService
          .findAllParticipants(
              projectId, body.status, body.company?.asCompanyId(), body.roles, pageable)
          .let { participantListResourceFactory.build(it, projectId) }
          .let { ok(it) }

  @GetMapping(ASSIGNABLE_PARTICIPANTS_BY_PROJECT_ID_ENDPOINT)
  open fun findAllAssignableParticipants(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestParam(name = "company", required = false) companyId: CompanyId?,
      @PageableDefault(
          sort = [USER_LAST_NAME, USER_FIRST_NAME], direction = ASC, size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<ParticipantListResource> =
      participantQueryService
          .findAllAssignableParticipants(projectId, companyId, pageable)
          .let { participantListResourceFactory.build(it, projectId) }
          .let { ok(it) }

  @GetMapping(COMPANIES_BY_PROJECT_ID_ENDPOINT)
  open fun findAllAssignableCompanies(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestParam(name = "includeInactive", defaultValue = "false") includeInactive: Boolean,
      pageable: Pageable
  ): ResponseEntity<ParticipantCompanyReferenceListResource> =
      participantQueryService
          .findAllAssignableCompanies(projectId, includeInactive, pageable)
          .let { participantCompanyListResourceFactory.build(it, includeInactive) }
          .let { ok(it) }

  @PutMapping(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT)
  open fun updateParticipant(
      @PathVariable(PATH_VARIABLE_PARTICIPANT_ID) participantId: ParticipantId,
      @RequestBody body: @Valid UpdateParticipantResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") @Nonnull eTag: ETag
  ): ResponseEntity<ParticipantResource> =
      updateParticipantCommandHandler
          .handle(UpdateParticipantCommand(participantId, eTag.toVersion(), body.role))
          .let { participantQueryService.findParticipantWithDetails(it)!! }
          .let { ok().eTag(it.toEtagString()).body(participantResourceFactory.build(it)) }

  @PostMapping(
      PARTICIPANTS_BY_PROJECT_ID_ENDPOINT, PARTICIPANT_BY_PROJECT_ID_AND_PARTICIPANT_ID_ENDPOINT)
  open fun assignParticipant(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @PathVariable(value = PATH_VARIABLE_PARTICIPANT_ID, required = false)
      participantId: ParticipantId?,
      @RequestBody body: @Valid AssignParticipantResource
  ): ResponseEntity<ParticipantResource> =
      assignParticipantCommandHandler
          .handle(AssignParticipantCommand(participantId, projectId, body.email, body.role))
          .let { participantQueryService.findParticipantWithDetails(it)!! }
          .let {
            created(participantLocation(projectId, it.identifier))
                .eTag(it.toEtagString())
                .body(participantResourceFactory.build(it))
          }

  @PostMapping(PARTICIPANTS_BY_PROJECT_ID_ADMIN_ENDPOINT)
  open fun assignParticipantAsAdmin(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody body: @Valid AssignParticipantAsAdminResource
  ): ResponseEntity<ParticipantResource> =
      assignParticipantAsAdminCommandHandler
          .handle(AssignParticipantAsAdminCommand(projectId, body.email))
          .let { participantQueryService.findParticipantWithDetailsAsAdmin(it)!! }
          .let {
            created(participantLocation(projectId, it.identifier))
                .eTag(it.toEtagString())
                .body(participantResourceFactory.build(it))
          }

  private fun participantLocation(projectId: ProjectId, participantId: ParticipantId) =
      fromCurrentContextPath()
          .path(getCurrentApiVersionPrefix())
          .path(PARTICIPANT_BY_PROJECT_ID_AND_PARTICIPANT_ID_ENDPOINT)
          .buildAndExpand(projectId, participantId)
          .toUri()

  @GetMapping(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT)
  open fun findParticipantById(
      @PathVariable(PATH_VARIABLE_PARTICIPANT_ID) participantId: ParticipantId
  ): ResponseEntity<ParticipantResource> =
      participantQueryService
          .findParticipantWithDetails(participantId)!!
          .let { participantResourceFactory.build(it) }
          .let { ok(it) }

  @DeleteMapping(PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT)
  open fun deleteParticipant(
      @PathVariable(PATH_VARIABLE_PARTICIPANT_ID) participantIdentifier: ParticipantId
  ): ResponseEntity<Void> =
      removeParticipantCommandHandler.handle(RemoveParticipantCommand(participantIdentifier)).let {
        noContent().build()
      }

  @PostMapping(PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT)
  open fun resendInvitation(
      @PathVariable(PATH_VARIABLE_PARTICIPANT_ID) participantId: ParticipantId
  ): ResponseEntity<Void> =
      resendInvitationCommandHandler.handle(ResendInvitationCommand(participantId)).let {
        noContent().build()
      }

  companion object {
    const val ASSIGNABLE_PARTICIPANTS_BY_PROJECT_ID_ENDPOINT =
        "/projects/{projectId}/participants/assignable"
    const val COMPANIES_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/companies"
    const val PARTICIPANTS_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/participants"
    const val PARTICIPANTS_BY_PROJECT_ID_ADMIN_ENDPOINT = "/projects/{projectId}/participants/admin"
    const val PARTICIPANTS_BY_PROJECT_SEARCH_ENDPOINT = "/projects/{projectId}/participants/search"
    const val PARTICIPANT_BY_PARTICIPANT_ID_RESEND_ENDPOINT =
        "/projects/participants/{participantId}/resend"
    const val PARTICIPANT_BY_PARTICIPANT_ID_ENDPOINT = "/projects/participants/{participantId}"
    const val PARTICIPANT_BY_PROJECT_ID_AND_PARTICIPANT_ID_ENDPOINT =
        "/projects/{projectId}/participants/{participantId}"
    const val PATH_VARIABLE_PARTICIPANT_ID = "participantId"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}
