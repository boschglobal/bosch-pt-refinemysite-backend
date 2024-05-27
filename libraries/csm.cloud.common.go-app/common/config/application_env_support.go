package config

import "github.com/spf13/viper"

/*
loadEnvVariables loads and maps an explicitly defined list of env variables to be
parsed into the configuration property structs.
*/
func loadEnvVariables() error {
	err := viper.BindEnv("kafka.consumer.groupId", "KAFKA_CONSUMER_GROUP_ID")
	if err != nil {
		return err
	}
	return nil
}
