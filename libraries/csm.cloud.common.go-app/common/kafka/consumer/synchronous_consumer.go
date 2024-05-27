package consumer

import (
	"context"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	commonKafka "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/consumer/deserializer"
	"errors"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/rs/zerolog/log"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
	"os"
	"os/signal"
	"slices"
	"strings"
	"syscall"
	"time"
)

type SynchronousKafkaConsumer struct {
	consumer     *kafka.Consumer
	deserializer deserializer.Deserializer
}

func NewSynchronousKafkaConsumer(
	consumer *kafka.Consumer,
	deserializer deserializer.Deserializer) SynchronousKafkaConsumer {
	return SynchronousKafkaConsumer{
		consumer:     consumer,
		deserializer: deserializer,
	}
}

func (this *SynchronousKafkaConsumer) Consume(consumerProperties properties.ConsumerProperties, topics []string, callback func(record commonKafka.Record) error) error {
	err := this.consumer.SubscribeTopics(topics, nil)
	if err != nil {
		return err
	}
	readTimeout, err := time.ParseDuration(consumerProperties.ReadTimeout)
	if err != nil {
		return err
	}

	// Register os signal SIGTERM listener to stop the consumption loop
	sigchan := make(chan os.Signal, 1)
	signal.Notify(sigchan, syscall.SIGINT, syscall.SIGTERM)

	go app.Run(func() {
		for {
			select {
			case sig := <-sigchan:
				log.Warn().Msg(fmt.Sprintf("SIGTERM signal received. Stopping kafka consumer loop: %s", sig))
				break
			default:
				log.Trace().Msg("No termination signal received")
			}

			// Read next message
			message, err := this.consumer.ReadMessage(readTimeout)

			if err == nil {
				this.handleMessage(message, callback)
			} else {
				var kafkaErr kafka.Error
				isKafkaError := errors.As(err, &kafkaErr)
				// Error codes can be found here:
				// https://github.com/confluentinc/librdkafka/blob/master/src/rdkafka.h
				if isKafkaError && kafkaErr.Code() == kafka.ErrorCode(-195) && strings.Contains(kafkaErr.String(), "Connection refused") {
					log.Debug().Msg("Connection to kafka broker refused. Retrying to connect...")
				} else if isKafkaError && kafkaErr.IsTimeout() {
					log.Trace().Msg("Kafka consumer timeout detected because no message was received")
				} else {
					panic(app.NewFatalError("Consumer is in an unrepairable state and needs to be terminated", err))
				}
			}
		}
	})

	return nil
}

func (this *SynchronousKafkaConsumer) handleMessage(message *kafka.Message, callback func(record commonKafka.Record) error) {

	// Initialize tracing from kafka headers
	datadogHeaders := make([]kafka.Header, 0)
	for _, header := range message.Headers {
		if slices.Contains(datadog.DatadogHeaders, header.Key) {
			datadogHeaders = append(datadogHeaders, header)
		}
	}

	parentContext, err := tracer.Extract(datadog.NewKafkaHeaderCarrierFromHeaders(datadogHeaders))
	if err != nil {
		log.Warn().Msg("Couldn't extract tracing header from kafka message: " + err.Error())
		parentContext = nil
	}

	// Set tracing context for traceHeader header from kafka message
	span := tracer.StartSpan("handleMessage", tracer.ChildOf(parentContext))
	tracingContext := tracer.ContextWithSpan(context.Background(), span)

	// Deserialize key
	key, err := datadog.TraceWithContext(tracingContext, "deserializeKey", func() (any, error) {
		return this.deserializer.Deserialize(message.Key)
	})
	if err != nil {
		span.Finish(tracer.WithError(err))
		panic(app.NewFatalError("Message key couldn't be deserialized", err))
	}

	// Deserialize event
	value, err := datadog.TraceWithContext(tracingContext, "deserializeValue", func() (any, error) {
		return this.deserializer.Deserialize(message.Value)
	})
	if err != nil {
		span.Finish(tracer.WithError(err))
		panic(app.NewFatalError("Message value couldn't be deserialized", err))
	}

	// Send event to callback channel
	_, err = datadog.TraceWithContext[any](tracingContext, "processMessage", func() (any, error) {
		err = callback(commonKafka.Record{
			Ctx: tracingContext,
			Message: commonKafka.DeserializedMessage{
				Headers:        message.Headers,
				TopicPartition: message.TopicPartition,
				Timestamp:      message.Timestamp,
				TimestampType:  message.TimestampType,
				Key:            key,
				Value:          value,
			},
		})
		return nil, err
	})
	if err != nil {
		span.Finish(tracer.WithError(err))
		panic(app.NewFatalError("Kafka message could not be processed", err))
	}

	// Commit offset
	_, err = datadog.TraceWithContext(tracingContext, "commitMessage", func() (any, error) {
		return this.consumer.CommitMessage(message)
	})
	if err != nil {
		span.Finish(tracer.WithError(err))
		panic(app.NewFatalError("Failed to commit kafka message", err))
	}
	span.Finish()
}
