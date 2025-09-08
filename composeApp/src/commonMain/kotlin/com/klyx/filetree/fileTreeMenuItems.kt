package com.klyx.filetree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboard
import com.klyx.core.LocalNotifier
import com.klyx.core.clipboard.clipEntryOf
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.openFile
import com.klyx.extension.api.Worktree
import com.klyx.menu.MenuItem
import com.klyx.menu.menu
import kotlinx.coroutines.launch

@Composable
fun rememberFileTreeMenuItems(
    node: FileTreeNode,
    worktree: Worktree,
    onNewDocument: (isDirectory: Boolean) -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
    onCut: () -> Unit = {},
    onPaste: () -> Unit = {},
): List<MenuItem> {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val notifier = LocalNotifier.current

    return remember {
        menu {
            group("filetree") {
                item("New File") {
                    onClick {
                        onNewDocument(false)
                    }
                }

                item("New Folder") {
                    onClick {
                        onNewDocument(true)
                    }
                }

                divider()

                item("Open in Default App") {
                    //shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.Enter))

                    onClick {
                        openFile(node.file)
                    }
                }

                item("Open in Terminal") {
                    onClick {
                        notifier.toast("Not implemented yet")
                    }
                }

                divider()

                item("Cut") {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.X))
                    onClick { onCut() }
                }

                item("Copy") {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.C))
                    onClick { onCopy() }
                }

                item("Paste") {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.V))
                    onClick { onPaste() }
                }

                divider()

                item("Copy Path") {
                    shortcut(keyShortcutOf(ctrl = true, alt = true, key = Key.C))

                    onClick {
                        scope.launch {
                            clipboard.setClipEntry(clipEntryOf(node.file.absolutePath))
                        }
                    }
                }

                item("Copy Relative Path") {
                    shortcut(keyShortcutOf(ctrl = true, alt = true, shift = true, key = Key.C))

                    onClick {
                        scope.launch {
                            val relativePath = run {
                                val normalizedRoot = worktree.rootFile.absolutePath.trimEnd('/', '\\')
                                val path = node.file.absolutePath
                                if (path.startsWith(normalizedRoot)) {
                                    path.removePrefix(normalizedRoot).trimStart('/', '\\')
                                } else {
                                    path
                                }
                            }

                            clipboard.setClipEntry(clipEntryOf(relativePath))
                        }
                    }
                }

                divider()

                item("Rename") {
                    shortcut(keyShortcutOf(Key.F2))

                    onClick { onRename() }
                }

                item("Delete") {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.Delete))

                    onClick { onDelete() }
                }
            }
        }.getValue("filetree")
    }
}
