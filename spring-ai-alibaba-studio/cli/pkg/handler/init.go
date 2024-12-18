package handler

import (
	"embed"
	"fmt"
	"io"
	"os"
	"path/filepath"

	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/static"
	"github.com/spf13/cobra"
)

func InitHandler(cmd *cobra.Command, args []string) {
	templateFiles := static.GetTemplateFiles()
	projectName, err := cmd.Flags().GetString("name")
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	if projectName == "" {
		fmt.Println("Error: Flag name must not be empty")
		return
	}
	wd, err := os.Getwd()
	if err != nil {
		fmt.Printf("Error: Failed to visit current working directory: %v\n", err)
		return
	}
	targetPath := filepath.Join(wd, projectName)
	if _, err := os.Stat(targetPath); !os.IsNotExist(err) {
		fmt.Printf("Error: Directory '%s' already exists\n", projectName)
		return
	}
	if err := os.Mkdir(targetPath, os.ModePerm); err != nil {
		fmt.Printf("Error: Failed to create directory '%s': %v\n", projectName, err)
		return
	}

	if err := copyEmbeddedTemplate(templateFiles, constant.TemplatePath, targetPath); err != nil {
		fmt.Printf("Error: Failed to generate project: %v\n", err)
		return
	}

	fmt.Printf("Project '%s' created successfully!\n", projectName)
	fmt.Printf("To run the project:\n  cd %s\n  mvn spring-boot:run\n", projectName)
}

func copyEmbeddedTemplate(templateFiles embed.FS, srcDir, destDir string) error {
	entries, err := templateFiles.ReadDir(srcDir)
	if err != nil {
		return fmt.Errorf("failed to read embedded template directory: %v", err)
	}

	for _, entry := range entries {
		srcPath := filepath.Join(srcDir, entry.Name())
		srcPath = filepath.ToSlash(srcPath)
		destPath := filepath.Join(destDir, entry.Name())

		if entry.IsDir() {
			if err := os.MkdirAll(destPath, os.ModePerm); err != nil {
				return fmt.Errorf("failed to create directory: %v", err)
			}
			if err := copyEmbeddedTemplate(templateFiles, srcPath, destPath); err != nil {
				return err
			}
		} else {
			if err := copyEmbeddedFile(templateFiles, srcPath, destPath); err != nil {
				return err
			}
		}
	}
	return nil
}

func copyEmbeddedFile(templateFiles embed.FS, srcFile, destFile string) error {
	data, err := templateFiles.ReadFile(srcFile)
	if err != nil {
		return fmt.Errorf("failed to read embedded file: %v", err)
	}

	output, err := os.Create(destFile)
	if err != nil {
		return fmt.Errorf("failed to create file: %v", err)
	}
	defer output.Close()

	if _, err := io.Writer.Write(output, data); err != nil {
		return fmt.Errorf("failed to write file: %v", err)
	}

	return nil
}
