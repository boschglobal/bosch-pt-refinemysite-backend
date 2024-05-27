/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.user.user.model.GenderEnum
import com.bosch.pt.csm.user.user.model.dto.UserWithEmployeeCompanySearchResultDto
import java.util.Date
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmployableUserProjectionRepository : JpaRepository<EmployableUserProjection, UserId> {

  fun findOneById(identifier: UserId): EmployableUserProjection?

  fun findOneByEmployeeIdentifier(employeeIdentifier: EmployeeId): EmployableUserProjection?

  fun findAllByCompanyIdentifier(companyIdentifier: CompanyId): List<EmployableUserProjection>

  fun deleteByEmployeeIdentifier(employeeIdentifier: EmployeeId)

  @Query(
      "SELECT new com.bosch.pt.csm.user.user.model.dto.UserWithEmployeeCompanySearchResultDto" +
          "(e.id, e.firstName, e.lastName, e.email, e.admin, e.locked, e.gender, " +
          "e.userCreatedDate, e.companyIdentifier, e.companyName, e.employeeIdentifier) " +
          "from EmployableUserProjection e " +
          "where (:userName is null or upper(e.userName) like upper(concat('%', :userName, '%'))) " +
          "and (:email is null or upper(e.email) like upper(concat('%', :email, '%'))) " +
          "and (:companyName is null or upper(e.companyName) like upper(concat('%', :companyName, '%'))) " +
          "and (:#{#restrictedCountries.size()} = 0 or e.userCountry in :restrictedCountries)")
  fun findAllBySearchCriteria(
      @Param("userName") name: String?,
      @Param("email") email: String?,
      @Param("companyName") companyName: String?,
      @Param("restrictedCountries") restrictedCountries: Set<IsoCountryCodeEnum>,
      pageable: Pageable
  ): Page<UserWithEmployeeCompanySearchResultDto>

  @Modifying
  @Query(
      "update EmployableUserProjection e set " +
          "e.firstName = :firstName, " +
          "e.lastName = :lastName, " +
          "e.userName = concat(:firstName, ' ', :lastName), " +
          "e.admin = :admin, " +
          "e.locked = :locked, " +
          "e.email = :email, " +
          "e.gender = :gender, " +
          "e.userCountry = :country, " +
          "e.userCreatedDate = :createdDate " +
          "where e.id = :identifier")
  fun updateUserAttributes(
      identifier: UserId,
      firstName: String?,
      lastName: String?,
      email: String,
      admin: Boolean,
      locked: Boolean,
      gender: GenderEnum?,
      country: IsoCountryCodeEnum?,
      createdDate: Date
  ): Int

  @Modifying
  @Query(
      "update EmployableUserProjection e set " +
          "e.companyName = :companyName, " +
          "e.companyIdentifier = :companyIdentifier, " +
          "e.employeeIdentifier = :employeeIdentifier, " +
          "e.employeeCreatedDate = :employeeCreatedDate " +
          "where e.id = :identifier")
  fun updateCompanyAndEmployeeAttributes(
      identifier: UserId,
      companyName: String,
      companyIdentifier: CompanyId,
      employeeIdentifier: EmployeeId,
      employeeCreatedDate: Date
  ): Int

  @Modifying
  @Query(
      "update EmployableUserProjection e set " +
          "e.companyName = :companyName " +
          "where e.companyIdentifier = :companyIdentifier")
  fun updateCompanyName(
      companyIdentifier: CompanyId,
      companyName: String,
  ): Int

  @Modifying
  @Query(
      "update EmployableUserProjection e set " +
          "e.companyName = null, " +
          "e.companyIdentifier = null, " +
          "e.employeeIdentifier = null " +
          "where e.employeeIdentifier = :employeeIdentifier")
  fun clearCompanyAndEmployeeAttributesByEmployeeIdentifier(employeeIdentifier: EmployeeId): Int
}
