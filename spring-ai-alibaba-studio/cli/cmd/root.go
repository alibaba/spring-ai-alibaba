// Copyright 2024 the original author or authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cmd

import (
	"fmt"
	"os"
	"strings"

	"github.com/alibaba/spring-ai-alibaba/cmd/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/cmd/ui"
	"github.com/alibaba/spring-ai-alibaba/pkg/api"
	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
)

const (
	defaultConfigFileName = ".spring-ai-alibaba"
	defaultConfigFileExt  = "json"
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

	rootCmd.PersistentFlags().StringVarP(&cfgFile, constant.ConfigFlag, "c", "", fmt.Sprintf("Config file path (default $HOME/%s.%s)", defaultConfigFileName, defaultConfigFileExt))
	rootCmd.PersistentFlags().StringP(constant.BaseURLFlag, "u", api.DefaultBaseURL, "Base URL for the Spring AI Alibaba Studio server") // order: command flag > env var > config file
	rootCmd.PersistentFlags().StringP(constant.OutputFlag, "o", string(printer.TablePrinterKind), fmt.Sprintf("Output format supported values: %v", strings.Join(printer.PrinterKindsAsString(), ", ")))

	rootCmd.Flags().BoolP(constant.VersionFlag, "v", false, "Print the version number of Spring AI Alibaba Studio")

	// add subcommands
	rootCmd.AddCommand(chatmodel.GetChatModelCmd())
	rootCmd.AddCommand(ui.GetUICmd())
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

		viper.AddConfigPath(home)
		viper.SetConfigType(defaultConfigFileExt)
		viper.SetConfigName(defaultConfigFileName)
	}

	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err != nil {
		// Check if the error is because the config file was not found
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			// Config file was found but another error was produced
			fmt.Fprintln(os.Stderr, "Failed loading config file:", viper.ConfigFileUsed(), err)
			os.Exit(1)
		}
	}

	viper.AutomaticEnv() // read in environment variables that match
	viper.BindPFlag(constant.BaseURLFlag, rootCmd.PersistentFlags().Lookup(constant.BaseURLFlag))
}
