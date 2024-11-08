package config

const (
	DefaultConfigFileName = ".spring-ai-alibaba"
	DefaultConfigFileExt  = "yaml"
)

var configInstance = &config{}

func GetConfigInstance() *config {
	return configInstance
}

type config struct {
	BaseURL string `mapstructure:"baseURL"`
}
