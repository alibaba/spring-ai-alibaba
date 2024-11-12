package cmd

import (
	"fmt"
	"os"
	"strings"

	"github.com/alibaba/spring-ai-alibaba/cmd/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/pkg/api"
	"github.com/alibaba/spring-ai-alibaba/pkg/config"
	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
)

var cfgFile string

// rootCmd represents the base command when called without any subcommands
var rootCmd = &cobra.Command{
	Use:   constant.RootCmdName,
	Short: "Command line interface for Spring AI Alibaba Studio",
	Long:  "\nCommand line interface for Spring AI Alibaba Studio\n" + constant.ASCIILOGO,
	// Uncomment the following line if your bare application
	// has an action associated with it:
	// Run: func(cmd *cobra.Command, args []string) { },
	Run: func(cmd *cobra.Command, args []string) {
		// Check if the version flag is set
		if version, _ := cmd.Flags().GetBool(constant.VersionFlag); version {
			fmt.Println(constant.Version)
			return
		}
		// if not
		cmd.Help()
	},
}

// Execute adds all child commands to the root command and sets flags appropriately.
// This is called by main.main(). It only needs to happen once to the rootCmd.
func Execute() {
	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	// Here you will define your flags and configuration settings.
	// Cobra supports persistent flags, which, if defined here,
	// will be global for your application.

	rootCmd.PersistentFlags().StringVarP(&cfgFile, constant.ConfigFlag, "c", "", fmt.Sprintf("Config file path (default $HOME/%s.%s)", config.DefaultConfigFileName, config.DefaultConfigFileExt))
	rootCmd.PersistentFlags().StringP(constant.BaseURLFlag, "u", api.DefaultBaseURL, "Base URL for the Spring AI Alibaba Studio server")
	rootCmd.PersistentFlags().StringP(constant.OutputFlag, "o", string(printer.TablePrinterKind), fmt.Sprintf("Output format supported values: %v", strings.Join(printer.PrinterKindsAsString(), ", ")))

	rootCmd.Flags().BoolP(constant.VersionFlag, "v", false, "Print the version number of Spring AI Alibaba Studio")

	// bind flags to viper
	viper.BindPFlag(constant.BaseURLFlag, rootCmd.PersistentFlags().Lookup(constant.BaseURLFlag))

	// add subcommands
	rootCmd.AddCommand(chatmodel.GetChatModelCmd())
}

// initConfig reads in config file and ENV variables if set.
func initConfig() {
	if cfgFile != "" {
		// Use config file from the flag.
		viper.SetConfigFile(cfgFile)
	} else {
		// Find home directory.
		home, err := os.UserHomeDir()
		cobra.CheckErr(err)

		// Search config in home directory with config.DefaultConfigFileName.
		viper.AddConfigPath(home)
		viper.SetConfigType(config.DefaultConfigFileExt)
		viper.SetConfigName(config.DefaultConfigFileName)
	}

	viper.AutomaticEnv() // read in environment variables that match

	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err == nil {
		fmt.Fprintln(os.Stderr, "Using config file:", viper.ConfigFileUsed())
	}
	// Unmarshal config into global config instance
	if err := viper.Unmarshal(config.GetConfigInstance()); err != nil {
		fmt.Fprintln(os.Stderr, "Unable to decode config into struct:", err)
	}
}
