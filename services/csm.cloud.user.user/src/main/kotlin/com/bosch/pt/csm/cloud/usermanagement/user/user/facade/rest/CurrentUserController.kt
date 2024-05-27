/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.RegisteredUserPrincipal
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.UnregisteredUserPrincipal
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.RegisterUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.CurrentUserController.Companion.CURRENT_USER_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory.CurrentUserResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.CreateCurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.UpdateCurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.CurrentUserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion(from = 3)
@RestController
@RequestMapping(path = [CURRENT_USER_ENDPOINT_PATH])
class CurrentUserController(
    private val updateUserCommandHandler: UpdateUserCommandHandler,
    private val registerUserCommandHandler: RegisterUserCommandHandler,
    private val userQueryService: UserQueryService,
    private val currentUserResourceFactory: CurrentUserResourceFactory
) {

  @PostMapping
  fun registerCurrentUser(
      @RequestBody @Valid body: CreateCurrentUserResource,
      @UnregisteredUserPrincipal unregisteredUser: UnregisteredUser
  ): ResponseEntity<CurrentUserResource> {

    registerUserCommandHandler.handle(
        body.toCommand(unregisteredUser.username, unregisteredUser.email))

    return userQueryService.findCurrentUserWithDetails().let {
      val location =
          ServletUriComponentsBuilder.fromCurrentContextPath()
              .path(LinkUtils.getCurrentApiVersionPrefix() + CURRENT_USER_ENDPOINT_PATH)
              .build()
              .toUri()

      ResponseEntity.created(location)
          .eTag(it.version.toString())
          .body(currentUserResourceFactory.build(it))
    }
  }

  @PutMapping
  fun updateCurrentUser(
      @RequestBody @Valid body: UpdateCurrentUserResource,
      @RegisteredUserPrincipal currentUser: User,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<CurrentUserResource> {

    updateUserCommandHandler.handle(body.toCommand(currentUser.identifier, eTag.toVersion()))

    return userQueryService.findCurrentUserWithDetails().let {
      ResponseEntity.ok().eTag(it.version.toString()).body(currentUserResourceFactory.build(it))
    }
  }

  @GetMapping
  fun getCurrentUser(
      @AuthenticationPrincipal userDetails: UserDetails?
  ): ResponseEntity<CurrentUserResource> =
      if (userDetails is UnregisteredUser) {
        ResponseEntity.notFound().build()
      } else {
        userQueryService.findCurrentUserWithDetails().let {
          ResponseEntity.ok().eTag(it.version.toString()).body(currentUserResourceFactory.build(it))
        }
      }

  companion object {
    const val CURRENT_USER_ENDPOINT_PATH = "/users/current"
  }
}
