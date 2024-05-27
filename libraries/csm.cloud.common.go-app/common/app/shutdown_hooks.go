package app

import (
	"fmt"
	"github.com/rs/zerolog/log"
	"os"
	"os/signal"
	"syscall"
)

func init() {
	listenForShutdownSignals()
}

var shutDownListeners []func()

/*
RegisterShutdownListener allows you to pass a listener function that will be executed when the application
is interrupted by a termination or kill Signal
*/
func RegisterShutdownListener(listener func()) {
	shutDownListeners = append(shutDownListeners, listener)
}

/*
listenForShutdownSignals is called on initialization and listens for Interrupt and Termination signals calling
shutdown listeners in an orderly fashion before exiting the application
*/
func listenForShutdownSignals() {

	log.Info().Msg("Listening for shutdown signals")

	// Register an OS signal listener to react on shutdown (Interrupt/SIGTERM) signals
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)

	// Execute the following goroutine once the SIGTERM signal was received
	go func() {
		for sig := range c {
			log.Info().Msg(fmt.Sprintf("Reacting to shut down Signal '%s'", sig))

			// Execute the shutdown listeners
			for _, shutDownListener := range shutDownListeners {
				shutDownListener()
			}
			log.Info().Msg("All shut-down listeners have completed. Exiting")

			// Exit the application explicitly, as we have caught and interrupted the shutdown signal
			// (which stops propagation)
			os.Exit(1)
		}
	}()
}
