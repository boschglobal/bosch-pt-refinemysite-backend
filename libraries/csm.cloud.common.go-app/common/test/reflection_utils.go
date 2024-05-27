package test

import (
	"reflect"
	"unsafe"
)

/*
SetFieldValueForTesting allows to inject a value to a private field of a given object (struct) using reflection.
Intended for test purposes only!
*/
func SetFieldValueForTesting(object any, field string, value any) {
	// Enable reflection for the given object
	reflectedObject := reflect.ValueOf(object)

	// Load the real object, if the object is a pointer
	for reflectedObject.Kind() == reflect.Ptr && !reflectedObject.IsNil() {
		reflectedObject = reflectedObject.Elem()
	}

	// Throw error if the dereferenced object is a pointer
	if !reflectedObject.CanAddr() {
		panic("cannot address object - did you pass a pointer?")
	}

	// Throw error if the dereferenced object isn't a struct
	if reflectedObject.Kind() != reflect.Struct {
		panic("cannot set field of non-struct object ")
	}

	// Get the field where to set the value
	reflectedField := reflectedObject.FieldByName(field)

	// Set value into field
	reflect.NewAt(reflectedField.Type(), unsafe.Pointer(reflectedField.UnsafeAddr())).Elem().Set(reflect.ValueOf(value))
}
