package admin

import (
	"csm.cloud.image.scale/config"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/admin"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/admin/configurer"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

func CreateTopicsIfNeeded(configuration config.Configuration) {
	scaledTopic := configuration.Kafka.Topic.Scaled

	if configuration.Kafka.Topic.AutoCreateTopics {
		topicSpecification := kafka.TopicSpecification{
			Topic:             scaledTopic.Name,
			NumPartitions:     scaledTopic.Partitions,
			ReplicationFactor: scaledTopic.ReplicationFactor,
			ReplicaAssignment: nil,
			Config:            nil,
		}

		topicSpecifications := make([]kafka.TopicSpecification, 0)
		topicSpecifications = append(topicSpecifications, topicSpecification)

		kafkaAdmin := configurer.ConfigureKafkaAdminClient(configuration.Kafka.Broker)
		admin.CreateTopics(kafkaAdmin, topicSpecifications)
	}
}
