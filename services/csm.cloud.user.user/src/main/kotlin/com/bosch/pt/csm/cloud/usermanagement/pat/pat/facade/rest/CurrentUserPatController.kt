/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.CreatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.DeletePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.UpdatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler.CreatePatCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler.DeletePatCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler.UpdatePatCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.request.CreateOrUpdatePatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatCreatedResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatIdResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory.PatBatchResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory.PatCreatedResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory.PatResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.query.PatQueryService
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath

@ApiVersion(from = 3)
@RestController
@RequestMapping(path = ["/users/current"])
class CurrentUserPatController(
    private val createPatCommandHandler: CreatePatCommandHandler,
    private val updatePatCommandHandler: UpdatePatCommandHandler,
    private val deletePatCommandHandler: DeletePatCommandHandler,
    private val patCreatedResourceFactory: PatCreatedResourceFactory,
    private val patBatchResourceFactory: PatBatchResourceFactory,
    private val patResourceFactory: PatResourceFactory,
    private val patQueryService: PatQueryService,
) {

  @GetMapping("/pats")
  fun getPats(
      @AuthenticationPrincipal userDetails: UserDetails
  ): ResponseEntity<BatchResponseResource<PatResource>> =
      ResponseEntity.ok(
          patBatchResourceFactory.build(
              patQueryService.findByImpersonatedUser((userDetails as User).identifier)))

  @PostMapping("/pats")
  fun createPat(
      @RequestBody @Valid requestBody: CreateOrUpdatePatResource,
      @AuthenticationPrincipal userDetails: UserDetails
  ): ResponseEntity<PatCreatedResource> {

    val patCreatedCommandResult =
        createPatCommandHandler.handle(
            CreatePatCommand(
                impersonatedUser = (userDetails as User).identifier,
                description = requestBody.description,
                scopes = requestBody.scopes,
                validForMinutes = requestBody.validForMinutes,
                type = PatTypeEnum.RMSPAT1))

    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix() + "/pats/{patId}")
            .buildAndExpand(patCreatedCommandResult.patId)
            .toUri()

    val pat = requireNotNull(patQueryService.findByPatId(patCreatedCommandResult.patId))

    return ResponseEntity.created(location)
        .eTag(pat.version.toString())
        .body(patCreatedResourceFactory.build(patCreatedCommandResult, pat))
  }

  @PutMapping("/pats/{patId}")
  fun updatePat(
      @RequestBody @Valid requestBody: CreateOrUpdatePatResource,
      @PathVariable patId: PatId,
      @AuthenticationPrincipal user: User,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag,
  ): ResponseEntity<PatResource> {

    updatePatCommandHandler.handle(
        UpdatePatCommand(
            patId = patId,
            version = eTag.toVersion(),
            impersonatedUser = user.identifier,
            description = requestBody.description,
            scopes = requestBody.scopes,
            validForMinutes = requestBody.validForMinutes,
        ))

    val pat = requireNotNull(patQueryService.findByPatId(patId))
    return ResponseEntity.ok().eTag(pat.version.toString()).body(patResourceFactory.build(pat))
  }

  @DeleteMapping("/pats/{patId}")
  fun deletePat(
      @PathVariable patId: PatId,
      @AuthenticationPrincipal user: User,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<PatIdResource> =
      ResponseEntity.ok()
          .body(
              PatIdResource(
                  deletePatCommandHandler.handle(
                      DeletePatCommand(
                          patId = patId,
                          version = eTag.toVersion(),
                          impersonatedUser = user.identifier,
                      ))))
}
