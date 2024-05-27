/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val PARTICIPANT_PROJECTION = "ParticipantProjection"

@Document(PARTICIPANT_PROJECTION)
@TypeAlias(PARTICIPANT_PROJECTION)
data class Participant(
    @Id val identifier: ParticipantId,
    val version: Long,
    val project: ProjectId,
    val company: CompanyId,
    val user: UserId,
    val role: ParticipantRoleEnum,
    val status: ParticipantStatusEnum,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<ParticipantVersion>
)

data class ParticipantVersion(
    val version: Long,
    val project: ProjectId,
    val company: CompanyId,
    val user: UserId,
    val role: ParticipantRoleEnum,
    val status: ParticipantStatusEnum,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime
)

enum class ParticipantRoleEnum(private val role: String) : TranslatableEnum {
  CR("COMPANY_REPRESENTATIVE"),
  CSM("SUPERINTENDENT"),
  FM("FOREMAN");

  companion object {
    const val KEY_PREFIX: String = "PARTICIPANT_ROLE_"
  }

  val shortKey: String
    get() = this.role

  override val key: String
    get() = "${KEY_PREFIX}${this.role}"

  override val messageKey: String
    get() = "${ParticipantRoleEnum::class.simpleName}_$this"
}

/*
 * Status INVITED and VALIDATION are filtered out in the listener.
 */
enum class ParticipantStatusEnum(private val status: String) : TranslatableEnum {
  // INVITED("INVITED"),

  // /**
  //  * the participant has finished his/her part of the registration process and is now awaiting
  //  * company assignment by the support team to became ACTIVE.
  //  */
  // VALIDATION,
  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE");

  companion object {
    const val KEY_PREFIX: String = "PARTICIPANT_STATUS_"
  }

  val shortKey: String
    get() = this.status

  override val key: String
    get() = "${KEY_PREFIX}${this.status}"

  override val messageKey: String
    get() = "${ParticipantStatusEnum::class.simpleName}_$this"
}

fun List<Participant>.projects() = map { it.project }
