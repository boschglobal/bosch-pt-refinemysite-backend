package avro

import (
	"bytes"
	"encoding/json"
	"github.com/riferrei/srclient"
)

type AvroTypeDeserializer interface {
	Handles(schemaId int) bool

	Deserialize(data []byte) (any, error)
}

type DefaultAvroTypeDeserializer[T comparable] struct {
	schema *srclient.Schema
}

func NewAvroTypeDeserializer[T comparable](schema *srclient.Schema) AvroTypeDeserializer {
	return &DefaultAvroTypeDeserializer[T]{
		schema: schema,
	}
}

func (this *DefaultAvroTypeDeserializer[T]) Handles(schemaId int) bool {
	return schemaId == this.schema.ID()
}

/*
Deserialize attempts to deserialize a binary representation into a type using the avro schema.
Returns either the deserialized struct or common.Option::None() and the error.
Implements deserializer.Deserializer interface.
*/
func (this *DefaultAvroTypeDeserializer[T]) Deserialize(data []byte) (any, error) {

	// Decode the binary into undecoded avro schema
	native, _, err := this.schema.Codec().NativeFromBinary(data)
	if err != nil {
		return nil, err
	}

	// Decode the undecoded avro into a textual representation
	textual, err := this.schema.Codec().TextualFromNative(nil, native)
	if err != nil {
		return nil, err
	}

	// Unmarshal it into the struct type
	var t T
	decoder := json.NewDecoder(bytes.NewReader(textual))
	decoder.DisallowUnknownFields()
	err = decoder.Decode(&t)
	if err != nil {
		return nil, err
	}

	return t, nil
}
