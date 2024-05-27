package properties

import "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"

type KafkaProperties struct {
	Broker         properties.BrokerProperties
	Schema         SchemaListProperties
	SchemaRegistry properties.SchemaRegistryProperties
	Topic          TopicsListProperties
}

type TopicsListProperties struct {
	AutoCreateTopics bool
	Upload           TopicProperties
}

type SchemaListProperties struct {
	AutoRegisterSchemas bool
	Key                 SchemaProperties
	Upload              SchemaProperties
}

type SchemaProperties struct {
	SchemaFile    string `validate:"required"`
	SchemaSubject string `validate:"required"`
}

type TopicProperties struct {
	Name              string `validate:"required"`
	Partitions        int    //optional
	ReplicationFactor int    //optional
}
