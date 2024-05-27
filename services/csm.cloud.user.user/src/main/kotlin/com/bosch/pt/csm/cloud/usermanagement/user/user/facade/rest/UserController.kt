/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.COMMON_VALIDATION_ERROR_SUGGEST_TERM_TOO_LONG
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.DeleteUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.GrantAdminPrivilegeCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.LockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.RevokeAdminPrivilegeCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UnlockUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.DeleteUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.GrantRevokeAdminPrivilegeCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.LockUnlockUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory.UserListResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory.UserReferencePageFactory
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory.UserResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserLockedResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request.SetUserRoleResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.SuggestionResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserListResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserReference
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion(from = 3)
@RestController
class UserController(
    private val deleteUserCommandHandler: DeleteUserCommandHandler,
    private val lockUnlockUserCommandHandler: LockUnlockUserCommandHandler,
    private val grantRevokeAdminPrivilegeCommandHandler: GrantRevokeAdminPrivilegeCommandHandler,
    private val userQueryService: UserQueryService,
    private val userReferencePageFactory: UserReferencePageFactory,
    private val userResourceFactory: UserResourceFactory,
    private val userListResourceFactory: UserListResourceFactory
) {

  @DeleteMapping(USER_BY_USER_ID_ENDPOINT_PATH)
  fun deleteUser(@PathVariable(PATH_VARIABLE_USER_ID) identifier: UserId): ResponseEntity<Any> {
    deleteUserCommandHandler.handle(DeleteUserCommand(identifier))
    return ResponseEntity.noContent().build()
  }

  @PostMapping(USER_ROLES_BY_USER_ID_ENDPOINT_PATH)
  fun setUserRole(
      @PathVariable(name = PATH_VARIABLE_USER_ID) userId: UserId,
      @RequestBody @Valid body: SetUserRoleResource
  ): ResponseEntity<UserResource> {
    if (body.admin) {
      grantRevokeAdminPrivilegeCommandHandler.handle(GrantAdminPrivilegeCommand(userId))
    } else {
      grantRevokeAdminPrivilegeCommandHandler.handle(RevokeAdminPrivilegeCommand(userId))
    }
    return userQueryService.findOneWithDetails(userId).let {
      ok().eTag(it.version.toString()).body(userResourceFactory.build(it))
    }
  }

  @PostMapping(USER_LOCK_BY_USER_ID_ENDPOINT_PATH)
  fun lockUser(
      @PathVariable(name = PATH_VARIABLE_USER_ID) userId: UserId,
      @RequestBody @Valid body: SetUserLockedResource
  ): ResponseEntity<UserResource> {
    if (body.locked) {
      lockUnlockUserCommandHandler.handle(LockUserCommand(userId))
    } else {
      lockUnlockUserCommandHandler.handle(UnlockUserCommand(userId))
    }
    return userQueryService.findOneWithDetails(userId).let {
      ok().eTag(it.version.toString()).body(userResourceFactory.build(it))
    }
  }

  @GetMapping(USERS_ENDPOINT_PATH)
  fun findAllUsers(
      @PageableDefault(sort = [User.LAST_NAME, User.FIRST_NAME], direction = Sort.Direction.ASC)
      pageable: Pageable
  ): ResponseEntity<UserListResource> {
    val users = userQueryService.findAllUsers(pageable)
    return ok(userListResourceFactory.build(users))
  }

  @GetMapping(USER_BY_USER_ID_ENDPOINT_PATH)
  fun findOneById(
      @PathVariable(PATH_VARIABLE_USER_ID) identifier: UserId
  ): ResponseEntity<UserResource> {
    val user = userQueryService.findOneWithDetails(identifier)
    return ok().eTag(user.version.toString()).body(userResourceFactory.build(user))
  }

  @PostMapping(USER_SUGGESTIONS_ENDPOINT_PATH)
  fun suggestUsersByTerm(
      @RequestBody suggestion: SuggestionResource,
      pageable: Pageable
  ): ResponseEntity<ListResponseResource<UserReference>> {
    if ((suggestion.term?.length ?: 0) > MAX_SUGGEST_TERM_SIZE) {
      throw PreconditionViolationException(COMMON_VALIDATION_ERROR_SUGGEST_TERM_TOO_LONG)
    }

    val users = userQueryService.suggestUsersByTerm(suggestion.term, pageable)
    return ok(userReferencePageFactory.buildForSuggestions(users))
  }

  companion object {
    const val USERS_ENDPOINT_PATH = "/users"
    const val USER_BY_USER_ID_ENDPOINT_PATH = "/users/{userId}"
    const val USER_ROLES_BY_USER_ID_ENDPOINT_PATH = "/users/{userId}/roles"
    const val USER_LOCK_BY_USER_ID_ENDPOINT_PATH = "/users/{userId}/lock"
    const val USER_SUGGESTIONS_ENDPOINT_PATH = "/users/suggestions"
    const val PATH_VARIABLE_USER_ID = "userId"
    private const val MAX_SUGGEST_TERM_SIZE = 200
  }
}
