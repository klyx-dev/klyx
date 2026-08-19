package com.klyx.i18n.strings

import com.klyx.i18n.I18nStrings

@I18nStrings(languageTag = "en", default = true)
object EnStrings : Strings {
    override val simple = "Hello!"

    override val projectOpened = "Project opened"
    override val invalidPath = { path: String -> "Invalid path: $path does not exist" }
    override val settingsExported = "Settings exported"
    override val exportFailed = { message: String? -> "Export failed: ${message ?: "Unknown error"}" }
    override val settingsImported = "Settings imported and applied"
    override val importFailedCouldNotRead = "Import failed: Could not read the selected file"
    override val importFailedInvalidSettings = "Import failed: The selected file does not contain valid settings"
    override val terminalEnvWiped = "Terminal environment wiped"
    override val bootstrapInstalledFromAssets = "Bootstrap installed from assets"
    override val installationFailed = { message: String? -> "Installation failed: ${message ?: "Unknown error"}" }
    override val noLogsToCopy = "No logs to copy"
    override val logsCopiedToClipboard = "Logs copied to clipboard"
    override val noLogsToShare = "No logs to share"
    override val bootstrapUpdated = "Bootstrap updated successfully"
    override val updateFailed = { message: String? -> "Update failed: ${message ?: "Unknown error"}" }
    override val bootstrapInstalled = "Bootstrap installed successfully"
    override val noTextSelectedToShare = "No text selected to share"

    override val screenNotRegistered = { id: String ->
        "Screen \"$id\" is not registered.\n\n" +
                "If you're a plugin developer, make sure to\n" +
                "register the screen via screens.register()\n" +
                "before navigating to it."
    }
    override val somethingWentWrong = { name: String? ->
        "Oops! Something went wrong.\nThis screen isn't available right now.\n\n($name)"
    }
    override val pluginCrashed = { id: String ->
        "Screen \"$id\" is not available because the plugin crashed.\n" +
                "Please open the Plugins settings to unload or reinstall it."
    }

    override val developerOptions = "Developer Options"
    override val back = "Back"
    override val terminalTesting = "Terminal Testing"
    override val wipeTerminalEnv = "Wipe Terminal Environment"
    override val wipeTerminalEnvDesc = "Deletes the prefix and version file to force a reinstall"
    override val logging = "Logging"
    override val viewAppLogs = "View App Logs"
    override val viewAppLogsDesc = "Browse in-app logs from plugins and system services"
    override val backupAndRestore = "Backup & Restore"
    override val exportSettings = "Export Settings"
    override val exportSettingsDesc = "Save all settings (appearance, editor, terminal, file tree) to a JSON file"
    override val importSettings = "Import Settings"
    override val importSettingsDesc = "Restore settings from a previously exported JSON file"
    override val debugTesting = "Debug Testing"
    override val installBootstrapFromAssets = "Install Bootstrap from Assets"
    override val installBootstrapFromAssetsDesc = "Wipes and extracts bootstrap binaries from APK assets"
    override val installFromAssetsQuestion = "Install from Assets?"
    override val installFromAssetsDesc = { assetName: String ->
        "This will wipe the current terminal environment and extract $assetName from the APK assets directory.\n\n" +
                "This is intended for testing bundled bootstrap archives."
    }
    override val cancel = "Cancel"
    override val install = "Install"
    override val installingBootstrap = "Installing Bootstrap"
    override val starting = "Starting..."

    override val newFile = "New File"
    override val newFolder = "New Folder"
    override val rename = "Rename"
    override val copyPath = "Copy Path"
    override val pasteHere = "Paste Here"
    override val delete = "Delete"
    override val copy = "Copy"
    override val cut = "Cut"
    override val paste = "Paste"
    override val deleteFile = "Delete File"
    override val openWith = "Open with"
    override val share = "Share"
    override val shareFile = "Share file"
    override val noAppToOpenFile = "No application found to open this file"
    override val couldNotOpenFile = { message: String? -> "Could not open file: ${message ?: "Unknown error"}" }
    override val noAppToShareFile = "No application available for sharing"
    override val couldNotShareFile = { message: String? -> "Could not share file: ${message ?: "Unknown error"}" }
    override val internalStorage = "Internal Storage"
    override val appData = "App Data"
    override val terminalHome = "Terminal Home"
    override val options = "OPTIONS"
    override val info = "INFO"
    override val name = "Name"
    override val path = "Path"
    override val size = "Size"
    override val lastModified = "Last modified"
    override val permissions = "Permissions"
    override val symbolicLink = "Symbolic link"
    override val calculating = "Calculating..."
    override val filesAndFolders = { files: Int, folders: Int -> "$files files, $folders folders" }
    override val closeProject = "Close Project"

    override val appLogs = "App Logs"
    override val copyLogs = "Copy logs"
    override val shareLogs = "Share logs"
    override val clearLogs = "Clear logs"
    override val noMatchingLogs = "No matching logs"
    override val noLogsYet = "No logs yet"
    override val all = "All"
    override val searchByTagOrMessage = "Search by tag or message..."
    override val clearSearch = "Clear search"
    override val shareLogsTitle = "Share Logs"
}
