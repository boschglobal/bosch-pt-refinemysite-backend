package datadog

import (
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"slices"
	"strings"
)

// TraceHeaderStringCarrier implements the propagator.TextMapReader interface for a string with trace header.
type TraceHeaderStringCarrier struct {
	traceHeader string
}

// NewTraceHeaderStringCarrier is a constructor to initialize the carrier with a trace header value.
// The expected format is: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}.
func NewTraceHeaderStringCarrier(traceHeader string) *TraceHeaderStringCarrier {
	return &TraceHeaderStringCarrier{traceHeader: traceHeader}
}

// ForeachKey implements the tracer.TextMapReader interface.
func (c *TraceHeaderStringCarrier) ForeachKey(handler func(key, val string) error) error {
	elements := strings.Split(c.traceHeader, "-")
	if len(elements) == 4 {
		// Trace header format is:
		// {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
		traceId := elements[0]
		parentId := elements[3]
		err := handler(DatadogTraceId, traceId)
		if err != nil {
			return err
		}
		err = handler(DatadogParentId, parentId)
		if err != nil {
			return err
		}
	}
	return nil
}

// KafkaHeaderCarrier implements the tracer.TextMapReader and propagator.TextMapWriter interface for a
// slice of kafka headers.
type KafkaHeaderCarrier struct {
	headers []kafka.Header
}

// NewKafkaHeaderCarrier implements the tracer.TextMapReader and propagator.TextMapWriter interface.
// This constructor can be used to initialize the carrier without values (e.g. to be used in a kafka producer).
func NewKafkaHeaderCarrier() *KafkaHeaderCarrier {
	return &KafkaHeaderCarrier{headers: make([]kafka.Header, 0)}
}

// NewKafkaHeaderCarrierFromHeaders implements the tracer.TextMapReader and propagator.TextMapWriter interface.
// This constructor can be used to initialize the carrier with values (e.g. to be used in a kafka consumer).
func NewKafkaHeaderCarrierFromHeaders(headers []kafka.Header) *KafkaHeaderCarrier {
	filteredHeaders := make([]kafka.Header, 0)
	for _, h := range headers {
		if slices.Contains(DatadogHeaders, h.Key) {
			filteredHeaders = append(filteredHeaders, h)
		}
	}
	return &KafkaHeaderCarrier{headers: filteredHeaders}
}

// Set implements the tracer.TextMapWriter interface which can be used in a kafka producer to inject
// datadog headers into kafka messages.
func (c *KafkaHeaderCarrier) Set(key, val string) {
	if slices.Contains(DatadogHeaders, key) {
		c.headers = append(c.headers, kafka.Header{
			Key:   key,
			Value: []byte(val),
		})
	}
}

// ForeachKey implements the tracer.TextMapReader interface which can be used to extract datadog headers
// from kafka message headers for a span context.
func (c *KafkaHeaderCarrier) ForeachKey(handler func(key, val string) error) error {
	for _, h := range c.headers {
		err := handler(h.Key, string(h.Value))
		if err != nil {
			return err
		}
	}
	return nil
}

// GetHeaders returns a list of headers stored in the carrier.
func (c *KafkaHeaderCarrier) GetHeaders() []kafka.Header {
	return c.headers
}

const DatadogParentId = "x-datadog-parent-id"
const DatadogSamplingPriority = "x-datadog-sampling-priority"
const DatadogTags = "x-datadog-tags"
const DatadogTraceId = "x-datadog-trace-id"

var DatadogHeaders = []string{DatadogParentId, DatadogSamplingPriority, DatadogTags, DatadogTraceId}
