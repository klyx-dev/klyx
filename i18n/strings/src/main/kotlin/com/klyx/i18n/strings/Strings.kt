package com.klyx.i18n.strings

interface Strings {
    val simple: String

    val projectOpened: String
    val invalidPath: (path: String) -> String
    val settingsExported: String
    val exportFailed: (message: String?) -> String
    val settingsImported: String
    val importFailedCouldNotRead: String
    val importFailedInvalidSettings: String
    val terminalEnvWiped: String
    val bootstrapInstalledFromAssets: String
    val installationFailed: (message: String?) -> String
    val noLogsToCopy: String
    val logsCopiedToClipboard: String
    val noLogsToShare: String
    val bootstrapUpdated: String
    val updateFailed: (message: String?) -> String
    val bootstrapInstalled: String
    val noTextSelectedToShare: String

    val screenNotRegistered: (id: String) -> String
    val somethingWentWrong: (name: String?) -> String
    val pluginCrashed: (id: String) -> String

    val developerOptions: String
    val back: String
    val terminalTesting: String
    val wipeTerminalEnv: String
    val wipeTerminalEnvDesc: String
    val logging: String
    val viewAppLogs: String
    val viewAppLogsDesc: String
    val backupAndRestore: String
    val exportSettings: String
    val exportSettingsDesc: String
    val importSettings: String
    val importSettingsDesc: String
    val debugTesting: String
    val installBootstrapFromAssets: String
    val installBootstrapFromAssetsDesc: String
    val installFromAssetsQuestion: String
    val installFromAssetsDesc: (assetName: String) -> String
    val cancel: String
    val install: String
    val installingBootstrap: String
    val starting: String

    val newFile: String
    val newFolder: String
    val rename: String
    val copyPath: String
    val pasteHere: String
    val delete: String
    val copy: String
    val cut: String
    val paste: String
    val deleteFile: String
    val openWith: String
    val share: String
    val shareFile: String
    val noAppToOpenFile: String
    val couldNotOpenFile: (message: String?) -> String
    val noAppToShareFile: String
    val couldNotShareFile: (message: String?) -> String
    val internalStorage: String
    val appData: String
    val terminalHome: String
    val options: String
    val info: String
    val name: String
    val path: String
    val size: String
    val lastModified: String
    val permissions: String
    val symbolicLink: String
    val calculating: String
    val filesAndFolders: (files: Int, folders: Int) -> String
    val closeProject: String

    val appLogs: String
    val copyLogs: String
    val shareLogs: String
    val clearLogs: String
    val noMatchingLogs: String
    val noLogsYet: String
    val all: String
    val searchByTagOrMessage: String
    val clearSearch: String
    val shareLogsTitle: String
}
