package datadog

import (
	"context"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
)

/*
TraceWithContext wraps the function call in a Datadog trace
*/
func TraceWithContext[T any](tracingContext context.Context, operationName string, function func() (T, error)) (T, error) {
	span, _, _ := StartSpan(operationName, tracingContext)
	result, err := function()
	span.Finish(tracer.WithError(err))
	return result, err
}

/*
StartSpan creates a span from the context passed in.
If the context contains a span, the new span will be created as a child of it.
Returns the span started as well as an error handle as a mere reference for deferred error handling.
*/
func StartSpan(operationName string, parentContext context.Context) (span ddtrace.Span, context context.Context, uninitializedErrorHandle error) {

	// The context should be consciously created or already exist in our calling code
	if parentContext == nil {
		panic("A context needs to be provided in StartSpan")
	}

	// Next to the span we must return the context returned here as a child context to the one passed in
	span, context = tracer.StartSpanFromContext(parentContext, operationName)

	return span, context, uninitializedErrorHandle
}
