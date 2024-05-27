/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.CreateUserResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.UserAdministrationListResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.UserAdministrationResource
import com.bosch.pt.iot.smartsite.dataimport.user.model.User
import com.bosch.pt.iot.smartsite.dataimport.user.rest.UserRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserImportService(
    private val userRestClient: UserRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<User> {

  private val existingUsers: MutableList<UserAdministrationResource> = ArrayList()

  private val existingUserIds: MutableMap<String, UUID> = HashMap()

  override fun importData(data: User) {
    authenticationService.selectUser(data.id)
    filterExistingUser(data)

    val typedUserId = TypedId.typedId(ResourceTypeEnum.user, data.id)
    if (idRepository.containsId(typedUserId)) {
      LOGGER.warn("Skipped existing user (id: " + data.id + ")")
      return
    }

    val createdUser = call { userRestClient.create(map(data)) }!!
    idRepository.store(typedUserId, createdUser.identifier)
  }

  fun getExistingUserId(userId: String): UUID? = existingUserIds[userId]

  fun loadExistingUsers() {
    authenticationService.selectAdmin()
    val page = AtomicInteger()
    var userListResource: UserAdministrationListResource

    do {
      userListResource = call { userRestClient.existingUsers(page.getAndIncrement()) }!!
      existingUsers.addAll(userListResource.users)
    } while (page.get() < userListResource.totalPages)
  }

  fun resetUserData() {
    existingUsers.clear()
    existingUserIds.clear()
  }

  private fun filterExistingUser(user: User) {
    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.user, user.id))) {
      return
    }

    val existingUser = existingUsers.firstOrNull { it.email == user.email }

    existingUser?.let {
      idRepository.store(TypedId.typedId(ResourceTypeEnum.user, user.id), it.identifier)
      existingUserIds[user.id] = it.identifier
    }
  }

  private fun map(user: User): CreateUserResource {
    val crafts: Set<String> = user.craftIds ?: emptySet()
    val craftIds: MutableSet<UUID> = HashSet()
    if (CollectionUtils.isNotEmpty(crafts)) {
      craftIds.addAll(crafts.map { idRepository[TypedId.typedId(ResourceTypeEnum.craft, it)]!! })
    }

    val gender = if (user.gender == null) null else user.gender.name
    return CreateUserResource(
        gender,
        user.firstName,
        user.lastName,
        user.email,
        user.position,
        user.roles,
        user.phoneNumbers,
        craftIds,
        true,
        user.locale,
        user.country)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserImportService::class.java)
  }
}
