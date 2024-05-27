/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphQlConfiguration {

  companion object {
    private const val STRING_INPUT_EXPECTED = "String input expected"
    private const val STRING_VALUE_EXPECTED = "StringValue expected"
  }

  @Bean
  fun runtimeWiringConfigurer(
      dateScalar: GraphQLScalarType,
      dateTimeScalar: GraphQLScalarType,
      decimalScalar: GraphQLScalarType,
      longScalar: GraphQLScalarType,
      uuidScalar: GraphQLScalarType
  ): RuntimeWiringConfigurer = RuntimeWiringConfigurer { builder ->
    builder
        .scalar(dateScalar)
        .scalar(dateTimeScalar)
        .scalar(decimalScalar)
        .scalar(longScalar)
        .scalar(uuidScalar)
  }

  @Bean
  fun dateScalar(): GraphQLScalarType =
      GraphQLScalarType.newScalar()
          .name("Date")
          .coercing(
              object : Coercing<LocalDate, String> {
                override fun serialize(
                    dataFetcherResult: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): String =
                    when (dataFetcherResult) {
                      is LocalDate -> dataFetcherResult.toString()
                      else -> throw CoercingSerializeException("LocalDate expected")
                    }

                override fun parseValue(
                    input: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): LocalDate =
                    when (input) {
                      is String ->
                          try {
                            LocalDate.parse(input)
                          } catch (ex: DateTimeParseException) {
                            throw CoercingParseValueException("Invalid Date: $input", ex)
                          }
                      else -> throw CoercingParseValueException(STRING_INPUT_EXPECTED)
                    }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): LocalDate =
                    when (input) {
                      is StringValue ->
                          try {
                            LocalDate.parse(input.value)
                          } catch (ex: DateTimeParseException) {
                            throw CoercingParseLiteralException(ex)
                          }
                      else -> throw CoercingParseValueException(STRING_VALUE_EXPECTED)
                    }
              })
          .build()

  @Bean
  fun dateTimeScalar(): GraphQLScalarType =
      GraphQLScalarType.newScalar()
          .name("DateTime")
          .coercing(
              object : Coercing<LocalDateTime, String> {
                override fun serialize(
                    dataFetcherResult: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): String =
                    when (dataFetcherResult) {
                      is LocalDateTime -> dataFetcherResult.toString()
                      else -> throw CoercingSerializeException("LocalDateTime expected")
                    }

                override fun parseValue(
                    input: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): LocalDateTime =
                    when (input) {
                      is String ->
                          try {
                            LocalDateTime.parse(input)
                          } catch (ex: DateTimeParseException) {
                            throw CoercingParseValueException("Invalid DateTime: $input", ex)
                          }
                      else -> throw CoercingParseValueException(STRING_INPUT_EXPECTED)
                    }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): LocalDateTime =
                    when (input) {
                      is StringValue ->
                          try {
                            LocalDateTime.parse(input.value)
                          } catch (ex: DateTimeParseException) {
                            throw CoercingParseLiteralException(ex)
                          }
                      else -> throw CoercingParseValueException(STRING_VALUE_EXPECTED)
                    }
              })
          .build()

  @Bean
  fun decimalScalar(): GraphQLScalarType =
      GraphQLScalarType.newScalar()
          .name("Decimal")
          .coercing(
              object : Coercing<BigDecimal, String> {
                override fun serialize(
                    dataFetcherResult: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): String =
                    when (dataFetcherResult) {
                      is BigDecimal -> dataFetcherResult.toString()
                      else -> throw CoercingSerializeException("LocalDateTime expected")
                    }

                override fun parseValue(
                    input: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): BigDecimal =
                    when (input) {
                      is String ->
                          try {
                            BigDecimal(input)
                          } catch (ex: NumberFormatException) {
                            throw CoercingParseValueException("Invalid Decimal: $input", ex)
                          }
                      else -> throw CoercingParseValueException(STRING_INPUT_EXPECTED)
                    }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): BigDecimal =
                    when (input) {
                      is StringValue ->
                          try {
                            BigDecimal(input.value)
                          } catch (ex: NumberFormatException) {
                            throw CoercingParseLiteralException(ex)
                          }
                      else -> throw CoercingParseValueException(STRING_VALUE_EXPECTED)
                    }
              })
          .build()

  @Bean
  fun longScalar(): GraphQLScalarType =
      GraphQLScalarType.newScalar()
          .name("Long")
          .coercing(
              object : Coercing<Long, String> {
                override fun serialize(
                    dataFetcherResult: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): String =
                    when (dataFetcherResult) {
                      is Long -> dataFetcherResult.toString()
                      else -> throw CoercingSerializeException("Long expected")
                    }

                override fun parseValue(
                    input: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): Long =
                    when (input) {
                      is String ->
                          try {
                            input.toLong()
                          } catch (ex: NumberFormatException) {
                            throw CoercingParseValueException("Invalid Long: $input", ex)
                          }
                      else -> throw CoercingParseValueException(STRING_INPUT_EXPECTED)
                    }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): Long =
                    when (input) {
                      is StringValue ->
                          try {
                            input.value.toLong()
                          } catch (ex: NumberFormatException) {
                            throw CoercingParseLiteralException(ex)
                          }
                      else -> throw CoercingParseValueException(STRING_VALUE_EXPECTED)
                    }
              })
          .build()

  @Bean
  fun uuidScalar(): GraphQLScalarType =
      GraphQLScalarType.newScalar()
          .name("UUID")
          .coercing(
              object : Coercing<UUID, String> {
                override fun serialize(
                    dataFetcherResult: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): String =
                    when (dataFetcherResult) {
                      is UUID -> dataFetcherResult.toString()
                      else -> throw CoercingSerializeException("UUID expected")
                    }

                override fun parseValue(
                    input: Any,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): UUID =
                    when (input) {
                      is String ->
                          try {
                            UUID.fromString(input)
                          } catch (ex: IllegalArgumentException) {
                            throw CoercingParseValueException("Invalid UUID: $input", ex)
                          }
                      else -> throw CoercingParseValueException(STRING_INPUT_EXPECTED)
                    }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): UUID =
                    when (input) {
                      is StringValue ->
                          try {
                            UUID.fromString(input.value)
                          } catch (ex: IllegalArgumentException) {
                            throw CoercingParseLiteralException(ex)
                          }
                      else -> throw CoercingParseValueException(STRING_VALUE_EXPECTED)
                    }
              })
          .build()
}
