/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor.getCurrentUserReference
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.CreatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MAX_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MIN_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatCreatedCommandResult
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatCreatedEvent
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore.PatSnapshot
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import java.security.SecureRandom
import java.time.LocalDateTime.now
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreatePatCommandHandler(private val eventBus: PatLocalEventBus) {

  @Suppress("SpringElInspection")
  @PreAuthorize("#command.impersonatedUser == principal.identifier")
  @Transactional
  fun handle(command: CreatePatCommand): PatCreatedCommandResult =
      PatId().let { patId ->
        GeneratedPat(
                patId = patId,
                type = command.type,
                impersonatedUser = command.impersonatedUser,
            )
            .let { generatedPat ->
              PatSnapshot(
                      identifier = patId,
                      impersonatedUser = command.impersonatedUser,
                      description = command.description,
                      hash = generatedPat.hash,
                      issuedAt = now(),
                      expiresAt = now().plusMinutes(command.validForMinutes),
                      scopes = command.scopes,
                      type = command.type)
                  .toCommandHandler()
                  .checkPrecondition {
                    command.validForMinutes in
                        MIN_PAT_VALIDITY_IN_MINUTES..MAX_PAT_VALIDITY_IN_MINUTES
                  }
                  .onFailureThrow(Key.PAT_VALIDATION_ERROR_VALIDITY_OUT_OF_BOUNDS)
                  .emitEvent {
                    PatCreatedEvent(
                        patId = it.identifier,
                        impersonatedUser = it.impersonatedUser,
                        scopes = it.scopes,
                        type = it.type,
                        description = it.description,
                        hash = it.hash,
                        issuedAt = it.issuedAt,
                        expiresAt = it.expiresAt,
                        userIdentifier = getCurrentUserReference().identifier,
                        timestamp = now(),
                    )
                  }
                  .to(eventBus)
              PatCreatedCommandResult(
                  patId = patId,
                  token = generatedPat.value,
              )
            }
      }

  /**
   * Generates a new PAT in the format
   *
   * _[type].[patId].secret_
   *
   * Example: _RMSPAT1.3834dd6d2c264dc8b7fe362ab9c1f127.vHfaBjGASEz20VLED01kI9AnxppCeWrz_
   *
   * PAT composition:
   * - _[type]_ is the PAT type. Currently only _[PatTypeEnum.RMSPAT1]_ is supported. PATs of
   *   different type may use different structure and means of hash generation.
   * - _[patId]_ is the UUID of the PAT stripped of its dashes.
   * - _secret_ is a generated alphanumeric value that should be only known to the impersonated
   *   user.
   *
   * _[hash]_ Is a bcrypt hash of the full plain token value. It is intended to be used to verify in
   * backend services that the plain token value submitted by a user is valid. The plain token value
   * is not intended to be stored or shared by backend services. _[BCrypt.hashpw]_ is used to
   * generate the hash. Use _[BCrypt.checkpw]_ for verification.
   */
  class GeneratedPat(val patId: PatId, val type: PatTypeEnum, val impersonatedUser: UserId) {

    private val keyPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    val value: String

    val hash: String

    init {
      value = generateValue()
      hash = BCrypt.hashpw(value, BCrypt.gensalt())
    }

    private fun generateValue(): String =
        type.toString() +
            TOKEN_DELIMITER +
            patId.identifier.toString().replace(UUID_DELIMITER, EMPTY_STRING) +
            TOKEN_DELIMITER +
            generateSecretKey()

    private fun generateSecretKey() =
        SecureRandom().let { secureRandom ->
          (1..SECRET_KEY_LENGTH)
              .map { secureRandom.nextInt(0, this.keyPool.size).let { keyPool[it] } }
              .joinToString(EMPTY_STRING)
        }

    companion object {
      private const val SECRET_KEY_LENGTH = 32
      private const val EMPTY_STRING = ""
      private const val TOKEN_DELIMITER = "."
      private const val UUID_DELIMITER = "-"
    }
  }
}
