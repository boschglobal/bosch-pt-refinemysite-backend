package schema_registry

import "github.com/riferrei/srclient"

type KnownSchemas struct {
	FileCreatedEvent  srclient.Schema
	ImageDeletedEvent srclient.Schema
	ImageScaledEvent  srclient.Schema
	StringMessageKey  srclient.Schema
	MessageKey        srclient.Schema
}
