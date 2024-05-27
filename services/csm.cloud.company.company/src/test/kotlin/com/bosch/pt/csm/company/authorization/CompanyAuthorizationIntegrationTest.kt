/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.authorization

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.company.query.CompanyQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable.unpaged

@DisplayName("Verify authorization of company service")
class CompanyAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CompanyQueryService

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `find all companies authorized for admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findAllCompanies(PageRequest.of(0, 10))).isNotNull()
    }
  }

  @Test
  fun `find all companies returns companies of authorized countries only if admin has country restrictions`() {
    authorizeWithAdministrativeUser(adminUS)
    cut.findAllCompanies(PageRequest.of(0, 10)).also {
      assertThat(it).hasSize(1)
      assertThat(it.content.first().identifier).isEqualTo(companyUS.identifier)
    }
  }

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `search all companies authorized for admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findCompaniesForFilters(null, PageRequest.of(0, 10))).isNotNull()
    }
  }

  @Test
  fun `search all companies returns those of authorized countries only if admin has country restrictions`() {
    authorizeWithAdministrativeUser(adminUS)
    cut.findCompaniesForFilters(null, PageRequest.of(0, 10)).also {
      assertThat(it).hasSize(1)
      assertThat(it.content.first().identifier).isEqualTo(companyUS.identifier)
    }
  }

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `find company by identifier authorized for admin only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findCompanyByIdentifier(companyDE.identifier)).isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `find company suggestions by term authorized for admin only`(userType: UserTypeAccess) {
    checkAccessWith(userType) { assertThat(cut.suggestCompaniesByTerm("", unpaged())).isNotNull() }
  }

  @Test
  fun `find company suggestions returns companies of authorized countries only if admin has country restrictions`() {
    authorizeWithAdministrativeUser(adminUS)
    cut.suggestCompaniesByTerm("", unpaged()).also {
      assertThat(it).hasSize(1)
      assertThat(it.content.first().identifier).isEqualTo(companyUS.identifier)
    }
  }
}
