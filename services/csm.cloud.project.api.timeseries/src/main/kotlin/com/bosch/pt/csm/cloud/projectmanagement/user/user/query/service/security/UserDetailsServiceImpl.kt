/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.security

import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository.UserProjectionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserProjectionRepository) :
    UserDetailsService {

  @Cacheable(cacheNames = ["user-by-idp-identifier"])
  override fun loadUserByUsername(idpIdentifier: String) =
      when {
        idpIdentifier.isBlank() -> throw UsernameNotFoundException("Empty userId given")
        else ->
            userRepository.findOneByIdpIdentifier(idpIdentifier)?.also {
              if (!it.isAccountNonLocked) {
                throw LockedException("The user account with userId $idpIdentifier is locked")
              }
            }
                ?: throw UsernameNotFoundException("No user found for userId $idpIdentifier")
      }
}
