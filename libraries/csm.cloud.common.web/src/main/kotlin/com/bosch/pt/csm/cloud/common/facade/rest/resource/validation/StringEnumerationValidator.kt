/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass
import org.springframework.stereotype.Component

/** Validator for enumerations. */
@Component
class StringEnumerationValidator : ConstraintValidator<StringEnumeration, Enum<*>> {

  private var availableEnumNames: Set<String> = HashSet()

  private fun getNamesSet(enumClass: KClass<out Enum<*>>): Set<String> {
    return enumClass.java.enumConstants.map { it.name }.toSet()
  }

  override fun initialize(stringEnumeration: StringEnumeration) {
    val enumSelected: KClass<out Enum<*>> = stringEnumeration.enumClass
    availableEnumNames = getNamesSet(enumSelected)
  }

  override fun isValid(value: Enum<*>?, context: ConstraintValidatorContext?): Boolean =
      value == null || availableEnumNames.contains(value.name)
}
