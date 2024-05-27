package datadog

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config"
	"github.com/rs/zerolog/log"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
)

/*
ApplyDefaultTracingConfiguration initializes the DatadogTracer if applicable (only in kubernetes profile) with
sensible defaults using the applications Log framework
*/
func ApplyDefaultTracingConfiguration() {

	if config.IsProfileActive("kubernetes") {

		// Configure datadog tracer and start it
		withJsonLogger := tracer.WithLogger(&Logger{})
		withRuntimeMetricsEnabled := tracer.WithRuntimeMetrics()
		withMaxSamplingRate := tracer.WithAnalytics(true)
		tracer.Start(
			withJsonLogger,
			withRuntimeMetricsEnabled,
			withMaxSamplingRate,
		)
		log.Info().Msg("DefaultTracingConfiguration: DatadogTracer started")

		// Register shutdown listener
		app.RegisterShutdownListener(func() {
			tracer.Stop()
			log.Info().Msg("DefaultTracingConfiguration: DatadogTracer stopped")
		})
	} else {
		log.Info().Msg("DefaultTracingConfiguration: no DatadogTracer available in this profile!")
	}
}
