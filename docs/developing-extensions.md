# Developing Extensions for Klyx

## Developing an Extension Locally

When developing an extension, you can use it in Klyx without needing to publish it by installing it as a _dev extension_.

From the extensions page, click the `Install Dev Extension` button and select the directory containing your extension.

If you already have a published extension with the same name installed, your dev extension will override it.

## Directory Structure of a Klyx Extension

A Klyx extension is a Git repository that contains an `extension.toml`. This file must contain some basic information about the extension:

```toml
id = "my-extension"
name = "My extension"
version = "0.0.1"
schema_version = 1
authors = ["Your Name <you@example.com>"]
description = "My cool extension"
repository = "https://github.com/your-name/my-klyx-extension"
```

In addition to this, there are several other optional files and directories that can be used to add functionality to a Klyx extension. An example directory structure of an extension that provides all capabilities is as follows:

```
my-extension/
  extension.toml
  lib/
    my-extension.wasm
  languages/
    my-language/
      config.toml
      highlights.scm
  themes/
    my-theme.json
```

### Publishing your extension

To publish an extension, open a PR to [the `klyx-dev/extensions` repo](https://github.com/klyx-dev/extensions).

> [!NOTE]
> It is very helpful if you fork the `klyx-dev/extensions` repo to a personal GitHub account instead of a GitHub organization, as this allows Klyx staff to push any needed changes to your PR to expedite the publishing process.

In your PR, do the following:

1. Add your extension as a Git submodule within the extensions/ directory

    ```shell
    git submodule add https://github.com/your-username/foobar-klyx.git extensions/foobar
    git add extensions/foobar
    ```

2. Add a new entry to the top-level `extensions.toml` file containing your extension:

    ```toml
    [my-extension]
    submodule = "extensions/my-extension"
    version = "0.0.1"
    ```

Once your PR is merged, the extension will be packaged and published to the Klyx extension registry.

> [!NOTE]
> Extension IDs and names should not contain `klyx` or `Klyx`, since they are all Klyx extensions.

### Updating an extension

To update an extension, open a PR to [the `klyx-dev/extensions` repo](https://github.com/klyx-dev/extensions).

In your PR do the following:

1. Update the extension's submodule to the commit of the new version.
2. Update the `version` field for the extension in `extensions.toml`
    - Make sure the `version` matches the one set in `extension.toml` at the particular commit.
