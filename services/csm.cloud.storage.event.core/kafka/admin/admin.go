package admin

import (
	"csm.cloud.storage.event.core/config"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/admin"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/admin/configurer"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

func CreateTopicsIfNeeded(configuration config.Configuration) {
	uploadTopic := configuration.Kafka.Topic.Upload

	if configuration.Kafka.Topic.AutoCreateTopics {
		topicSpecification := kafka.TopicSpecification{
			Topic:             uploadTopic.Name,
			NumPartitions:     uploadTopic.Partitions,
			ReplicationFactor: uploadTopic.ReplicationFactor,
			ReplicaAssignment: nil,
			Config:            nil,
		}

		topicSpecifications := make([]kafka.TopicSpecification, 0)
		topicSpecifications = append(topicSpecifications, topicSpecification)

		kafkaAdmin := configurer.ConfigureKafkaAdminClient(configuration.Kafka.Broker)
		admin.CreateTopics(kafkaAdmin, topicSpecifications)
	}
}
