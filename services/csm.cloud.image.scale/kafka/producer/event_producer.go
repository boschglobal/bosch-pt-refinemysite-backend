package producer

import (
	"context"
	"csm.cloud.image.scale/domain"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/partitioner/any"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer/avro"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/riferrei/srclient"
)

type Identifiable interface {
	GetIdentifier() string
}

type EventKafkaProducer[K domain.MessageKey, V Identifiable] interface {
	Produce(tracingContext context.Context, key K, event V) error
}

/*
defaultEventKafkaProducer produces events to Kafka
Refer to Produce method below for ConfluentKafka functionality.
*/
type defaultEventKafkaProducer[K domain.MessageKey, V Identifiable] struct {
	kafkaProducer producer.SynchronousKafkaProducer
}

/*
NewAvroEventKafkaProducer creates a fully initialized defaultEventKafkaProducer that
can be used to produce Avro records to Kafka.
*/
func NewAvroEventKafkaProducer[K domain.MessageKey, V Identifiable](
	keySchema *srclient.Schema,
	valueSchema *srclient.Schema,
	kafkaProducer *kafka.Producer,
	topic string) defaultEventKafkaProducer[K, V] {

	// Read topic metadata from kafka cluster
	metadata, err := kafkaProducer.GetMetadata(&topic, false, 10000)
	if err != nil {
		panic(app.NewFatalError("Unable to read topic metadata", err))
	}

	// Get partition count from topic metadata
	topicMetadata := metadata.Topics[topic]
	partitions := len(topicMetadata.Partitions)

	// Create topic / partition mapping
	topicPartitionMapping := make(map[string]int32)
	topicPartitionMapping[topic] = int32(partitions)

	// Return instantiated kafka producer
	return defaultEventKafkaProducer[K, V]{
		kafkaProducer: producer.NewSynchronousKafkaProducer(
			kafkaProducer,
			avro.NewAvroSerializer(keySchema),
			avro.NewAvroSerializer(valueSchema),
			any.NewMurmur2Partitioner(topicPartitionMapping),
			topic,
		),
	}
}

/*
Produce - produces a Kafka record in a synchronous fashion.
The method will either return without error having produced the Kafka record successfully
or return an error. Possible errors are serialization error, communication errors.
*/
func (this *defaultEventKafkaProducer[K, V]) Produce(tracingContext context.Context, key K, event V) error {
	return this.kafkaProducer.Produce(tracingContext, key, event)
}
