package properties

import "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"

type KafkaProperties struct {
	Broker         properties.BrokerProperties
	Consumer       properties.ConsumerProperties
	Schema         SchemaListProperties
	SchemaRegistry properties.SchemaRegistryProperties
	Topic          TopicsListProperties
}

type TopicsListProperties struct {
	AutoCreateTopics bool
	Uploaded         TopicProperties
	Scaled           TopicProperties
}

type SchemaListProperties struct {
	AutoRegisterSchemas bool
	Key                 SchemaProperties
	StringKey           SchemaProperties
	Deleted             SchemaProperties
	Uploaded            SchemaProperties
	Scaled              SchemaProperties
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
