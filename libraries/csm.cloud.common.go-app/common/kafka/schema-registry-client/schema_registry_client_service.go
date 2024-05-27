package schema_registry_client

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"fmt"
	"github.com/riferrei/srclient"
	"github.com/rs/zerolog/log"
)

/*
SchemaRegistryService provides an interface for a service that acts as a schema registry client
*/
type SchemaRegistryService interface {
	LoadAvroSchema(subject string,
		schemaBytes []byte) (*srclient.Schema, error)
}

/*
defaultSchemaRegistryService implements SchemaRegistryService.
Use NewDefaultSchemaRegistryService to create an instance
*/
type defaultSchemaRegistryService struct {
	autoRegisterSchemas  bool
	schemaRegistryConfig properties.SchemaRegistryProperties
	schemaRegistryClient *srclient.SchemaRegistryClient
}

/*
NewDefaultSchemaRegistryService creates a default instance of SchemaRegistryService
*/
func NewDefaultSchemaRegistryService(
	autoRegisterSchemas bool,
	schemaRegistryConfig properties.SchemaRegistryProperties) SchemaRegistryService {

	// Initialize schema registry client
	schemaRegistryClient := srclient.CreateSchemaRegistryClient(schemaRegistryConfig.Urls)

	// Set credentials on the client
	if schemaRegistryConfig.Api.Key != "" && schemaRegistryConfig.Api.Secret != "" {
		schemaRegistryClient.SetCredentials(
			schemaRegistryConfig.Api.Key,
			schemaRegistryConfig.Api.Secret)
	}

	// Return instance of "defaultSchemaRegistryService"
	return &defaultSchemaRegistryService{
		autoRegisterSchemas:  autoRegisterSchemas,
		schemaRegistryConfig: schemaRegistryConfig,
		schemaRegistryClient: schemaRegistryClient,
	}
}

/*
LoadAvroSchema initializes a Schema with the schema registry.
The schema content is loaded from schemaFile in local resources.

A version comparison with the latest schema for the subject passed in
helps to identify obsolete application deployments (warns if a newer schema is registered).

Honors autoRegisterSchema flag to decide if a schema not found in the registry should be
registered or if the application will terminate instead (expected production behavior).
*/
func (this *defaultSchemaRegistryService) LoadAvroSchema(
	subject string,
	schemaBytes []byte,
) (*srclient.Schema, error) {

	// Do a lookup to check if the schema already exists
	schema, err := this.schemaRegistryClient.LookupSchema(subject, string(schemaBytes), srclient.Avro)

	// Check if schema should be auto registered or return error
	if schema == nil {

		// Auto-register schema if desired by the user
		if this.autoRegisterSchemas {
			registeredSchema, registerError := this.schemaRegistryClient.CreateSchema(subject, string(schemaBytes), srclient.Avro)
			if registerError != nil {
				log.Error().Msg(fmt.Sprintf("Error registering the schema %s", registerError))
				return nil, registerError
			}
			return registeredSchema, nil
		} else {
			if err != nil {
				return this.logSchemaNotFoundReturnError(subject, err)
			}
			return nil, err
		}
	}

	if err != nil {
		return this.logSchemaNotFoundReturnError(subject, err)
	}
	log.Info().Msg(fmt.Sprintf("Found schema %d for %s", schema.ID(), subject))

	// Find the latest version of schema
	latestSchema, err := this.schemaRegistryClient.GetLatestSchema(subject)
	if err != nil {
		panic(err)
	}
	// Compare local schema version with the latest schema version and print error if they do not match
	if latestSchema != nil && (latestSchema.Version() != schema.Version()) {
		log.Warn().Msg(fmt.Sprintf("Schema supplied for record value of %s is registered as "+
			"id %d version %d which does not match latest schema registered with id %d version %d",
			subject, schema.ID(), schema.Version(), latestSchema.ID(), latestSchema.Version()))
	}

	return schema, nil
}

/*
logSchemaNotFoundReturnError logs the error with details and returns it back.
*/
func (this *defaultSchemaRegistryService) logSchemaNotFoundReturnError(subject string, err error) (*srclient.Schema, error) {
	log.Error().AnErr("error", err).Msg(fmt.Sprintf("Error looking up schema for subject %s: %s", subject, err.Error()))
	return nil, err
}
