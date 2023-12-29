# PyCharm Explain Method Plugin

## Description

The PyCharm Explain Method Plugin is a tool designed as part of an internship application for JetBrains. The purpose of this plugin is to enhance the PyCharm IDE's functionality by providing a single intention: to explain a selected method or function in Python code. If a portion of the method is selected, the plugin will explain the entire method. If the selection is outside the method, it will either do nothing or display an error message.

The plugin leverages the ChatGPT API (or any other Language Model API) to generate explanations. In cases where obtaining an API key for the Language Model is complicated, the plugin also offers the option to mock Language Model answers. The primary task of the plugin is to gather a sufficiently compact context, ensuring it remains under 8,192 tokens. This may involve compressing long methods, including definitions of references if they are outside of the method, and more.

The result of the explanation is displayed in a tool window.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)

## Installation

To use the PyCharm Explain Method Plugin, you need to build it and then install it in PyCharm. Here are the steps:

1. Clone the GitHub repository: [GitHub Repo Link](https://github.com/alexojica/jetbrains-llm-pycharm-plugin/)
2. Open the project in IntelliJ IDEA.
3. Build the plugin by navigating to "Gradle" > "Build"
4. Install the plugin in PyCharm by navigating to the PyCharm settings and selecting "Plugins." Click on "Install Plugin from Disk" and select the built plugin file. The file is located in the project's root directory under "build/distributions."
5. Restart PyCharm to activate the plugin.

## Usage

Once the plugin is installed, you can use it within the PyCharm IDE. Here's how to use it:

1. Open a Python code file in PyCharm.
2. Select a method or code snippet inside a method.
3. Trigger the "Explain Method" intention, which can be found in the right-click menu.
4. The plugin will generate an explanation for the selected method or function.

Ensure that your selection is within the method's boundaries for the plugin to work correctly. If the selected code is outside the method, it may not produce meaningful results.

## Configuration

The PyCharm Explain Method Plugin may require configuration depending on your specific use case. Here are some configuration options:

- **Language Model API**: If you choose to use a specific Language Model API (e.g., ChatGPT), you may need to configure the API key and endpoint.


## Acknowledgments

- JetBrains - For providing the internship opportunity and inspiring this project.
- [ChatGPT API](https://platform.openai.com/docs/) - For powering the language model used in this plugin.

