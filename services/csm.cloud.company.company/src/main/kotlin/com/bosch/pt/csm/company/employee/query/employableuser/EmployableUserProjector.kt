/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.api.toUserId
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getCreatedDate
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedDate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.user.user.model.GenderEnum
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class EmployableUserProjector(
    private val repository: EmployableUserProjectionRepository,
    private val companyNameRepository: EmployableUserCompanyNameRepository,
    private val ignoredCiamUsers: IgnoredCiamUsersProperties,
    private val logger: Logger
) {

  fun onUserCreatedEvent(aggregate: UserAggregateAvro) {
    if (aggregate.registered) {
      onUserUpdatedEvent(aggregate)
    }
  }

  fun onUserUpdatedEvent(aggregate: UserAggregateAvro) {
    if (aggregate.email !in ignoredCiamUsers.users) {
      when (repository.findOneById(aggregate.getIdentifier().asUserId())) {
        null -> aggregate.toNewProjection().apply { repository.save(this) }
        else -> updateProjectionFromUserAggregate(aggregate)
      }
    }
  }

  fun onUserDeletedEvent(key: AggregateEventMessageKey) {
    if (repository.existsById(key.aggregateIdentifier.identifier.asUserId())) {
      repository.deleteById(key.aggregateIdentifier.identifier.asUserId())
    }
  }

  fun onCompanyCreatedEvent(aggregate: CompanyAggregateAvro) {
    if (!companyNameRepository.existsById(aggregate.getIdentifier().asCompanyId())) {
      EmployableUserCompanyName(aggregate.getIdentifier().asCompanyId(), aggregate.name).apply {
        companyNameRepository.save(this)
      }
    }
  }

  fun onCompanyUpdatedEvent(aggregate: CompanyAggregateAvro) {
    companyNameRepository
        .findOneById(aggregate.getIdentifier().asCompanyId())!!
        .apply { this.companyName = aggregate.name }
        .apply { companyNameRepository.save(this) }
    repository.updateCompanyName(aggregate.getIdentifier().asCompanyId(), aggregate.name)
  }

  fun onCompanyDeletedEvent(aggregate: CompanyAggregateAvro) {
    if (companyNameRepository.existsById(aggregate.getIdentifier().asCompanyId())) {
      companyNameRepository.deleteById(aggregate.getIdentifier().asCompanyId())
    }
  }

  fun onEmployeeCreatedEvent(aggregate: EmployeeAggregateAvro) {
    val companyName = getCompanyName(aggregate.company.identifier.toUUID().asCompanyId())
    when (val projection = repository.findOneById(aggregate.user.toUserId())) {
      null -> aggregate.toNewProjection(companyName).apply { repository.save(this) }
      else ->
          when (isNewerThanAppliedEmployee(aggregate, projection)) {
            true -> updateProjectionFromEmployeeAggregate(aggregate, companyName)
            false -> logger.debug("Already applied employee created event newer than received one")
          }
    }
  }

  private fun isNewerThanAppliedEmployee(
      aggregate: EmployeeAggregateAvro,
      projection: EmployableUserProjection
  ) =
      projection.employeeCreatedDate == null ||
          aggregate.getCreatedDate().toDate().after(projection.employeeCreatedDate)

  private fun getCompanyName(companyId: CompanyId) =
      companyNameRepository.findOneById(companyId)!!.companyName

  fun onEmployeeDeletedEvent(aggregate: EmployeeAggregateAvro) {
    val employeeId = aggregate.getIdentifier().asEmployeeId()
    repository.findOneByEmployeeIdentifier(employeeId)?.apply {
      when (firstName) {
        null -> repository.deleteByEmployeeIdentifier(employeeId)
        else -> repository.clearCompanyAndEmployeeAttributesByEmployeeIdentifier(employeeId)
      }
    }
  }

  private fun UserAggregateAvro.toNewProjection() =
      EmployableUserProjection(
          id = this.getIdentifier().asUserId(),
          firstName = this.firstName,
          lastName = this.lastName,
          userName = "${this.firstName} ${this.lastName}",
          email = this.email,
          admin = this.admin,
          locked = this.locked,
          gender = if (this.gender != null) GenderEnum.valueOf(this.gender.name) else null,
          userCreatedDate = this.getCreatedDate().toDate(),
          userCountry =
              if (this.country != null) IsoCountryCodeEnum.valueOf(this.country.name) else null)

  private fun EmployeeAggregateAvro.toNewProjection(companyName: String) =
      EmployableUserProjection(
          id = user.toUserId(),
          employeeIdentifier = getIdentifier().asEmployeeId(),
          employeeCreatedDate = this.getCreatedDate().toDate(),
          companyIdentifier = company.identifier.toUUID().asCompanyId(),
          companyName = companyName)

  private fun updateProjectionFromUserAggregate(aggregate: UserAggregateAvro) =
      repository.updateUserAttributes(
          aggregate.getIdentifier().asUserId(),
          aggregate.firstName,
          aggregate.lastName,
          aggregate.email,
          aggregate.admin,
          aggregate.locked,
          GenderEnum.valueOf(aggregate.gender.name),
          if (aggregate.country != null) IsoCountryCodeEnum.valueOf(aggregate.country.name)
          else null,
          aggregate.getCreatedDate().toDate())

  private fun updateProjectionFromEmployeeAggregate(
      aggregate: EmployeeAggregateAvro,
      companyName: String
  ) =
      repository.updateCompanyAndEmployeeAttributes(
          aggregate.user.toUserId(),
          companyName,
          aggregate.company.identifier.toUUID().asCompanyId(),
          aggregate.getIdentifier().asEmployeeId(),
          aggregate.getCreatedDate().toDate())
}
