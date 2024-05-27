package producer

import (
	"context"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/partitioner"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/rs/zerolog/log"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
)

/*
SynchronousKafkaProducer is a confluent Kafka producer instance with schema registry support.
Use NewSynchronousKafkaProducer to create an instance.
*/
type SynchronousKafkaProducer struct {
	producer        *kafka.Producer
	keySerializer   serializer.Serializer
	valueSerializer serializer.Serializer
	partitioner     partitioner.Partitioner
	topicName       string
}

/*
NewSynchronousKafkaProducer - creates a fully initialized FileCreatedEventKafkaProducer that can be used
to produce FileCreatedEvent representations as Avro records to Kafka.
*/
func NewSynchronousKafkaProducer(
	producer *kafka.Producer,
	keySerializer serializer.Serializer,
	valueSerializer serializer.Serializer,
	partitioner partitioner.Partitioner,
	topic string,
) SynchronousKafkaProducer {
	return SynchronousKafkaProducer{
		producer:        producer,
		keySerializer:   keySerializer,
		valueSerializer: valueSerializer,
		partitioner:     partitioner,
		topicName:       topic,
	}
}

/*
Produce - produces to Kafka topicName
*/
func (this *SynchronousKafkaProducer) Produce(tracingContext context.Context, key any, value any) error {

	// Serialize key
	recordKey, err := this.keySerializer.Serialize(key)
	if err != nil {
		return err
	}

	// Serialize value
	recordValue, err := this.valueSerializer.Serialize(value)
	if err != nil {
		return err
	}

	// Create channel get the result of the asynchronous Produce operation
	deliveryChan := make(chan kafka.Event, 10000)

	// Calculate kafka partition
	partition := this.partitioner.Partition(this.topicName, key, recordKey, value, recordValue)
	topicPartition := kafka.TopicPartition{
		Topic:     &this.topicName,
		Partition: partition,
	}

	// Calculate tracing headers
	var headers []kafka.Header
	if span, hasSpan := tracer.SpanFromContext(tracingContext); hasSpan {
		carrier := datadog.NewKafkaHeaderCarrier()
		err := tracer.Inject(span.Context(), carrier)
		if err != nil {
			return err
		}
		headers = carrier.GetHeaders()
	} else {
		headers = nil
	}

	// Send record asynchronously
	err = this.producer.Produce(&kafka.Message{
		Headers:        headers,
		TopicPartition: topicPartition,
		Key:            recordKey,
		Value:          recordValue,
	}, deliveryChan)
	if err != nil {
		return err
	}

	// Block until the asynchronous Produce operation is finished and a result is available
	e := <-deliveryChan

	// Cast the result to Message type and close the channel
	m := e.(*kafka.Message)
	close(deliveryChan)

	// Handle errors
	if m.TopicPartition.Error != nil {
		log.Warn().Msg(fmt.Sprintf("Delivery of kafka message failed: %v\n", m.TopicPartition.Error))
		return m.TopicPartition.Error
	} else {
		log.Debug().Msg(fmt.Sprintf("Delivered kafka message to topic %s in partition %d at offset %v",
			*m.TopicPartition.Topic, m.TopicPartition.Partition, m.TopicPartition.Offset))
		return nil
	}
}
