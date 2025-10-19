package com.klyx.editor.compose.selection.contextmenu

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import com.klyx.editor.compose.selection.contextmenu.ProcessTextApi23Impl.queryProcessTextActivities
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope
import com.klyx.editor.compose.selection.contextmenu.builder.item
import com.klyx.editor.compose.selection.contextmenu.data.ProcessTextKey

@SuppressLint("ObsoleteSdkInt")
@OptIn(ExperimentalFoundationApi::class)
internal fun TextContextMenuBuilderScope.addProcessedTextContextMenuItems(
    context: Context,
    editable: Boolean,
    text: CharSequence,
    selection: TextRange,
) {
    if (
        !ComposeFoundationFlags.isSmartSelectionEnabled ||
        selection.collapsed ||
        text.isEmpty() ||
        Build.VERSION.SDK_INT < 23
    ) {
        return
    }

    val packageManager = context.packageManager
    val resolveInfos = queryProcessTextActivities(context)
    if (resolveInfos.isEmpty()) return

    separator()
    resolveInfos.fastForEachIndexed { index, resolveInfo ->
        item(
            key = ProcessTextKey(index),
            label = resolveInfo.loadLabel(packageManager).toString(),
            onClick = {
                ProcessTextApi23Impl.onClickProcessTextItem.invoke(
                    context,
                    resolveInfo,
                    editable,
                    text,
                    selection,
                )
                close()
            },
        )
    }
    separator()
}

@RequiresApi(23)
internal object ProcessTextApi23Impl {
    @VisibleForTesting
    var processTextActivitiesQuery: (context: Context) -> List<ResolveInfo> = { context ->
        context.packageManager.queryIntentActivities(createProcessTextIntent(), 0).fastFilter { info
            ->
            info.hasPermission(context)
        }
    }

    @VisibleForTesting
    var onClickProcessTextItem:
                (
        context: Context,
        resolveInfo: ResolveInfo,
        editable: Boolean,
        text: CharSequence,
        textRange: TextRange,
    ) -> Unit =
        { context, resolveInfo, editable, text, selection ->
            val selectedText = text.substring(selection.min, selection.max)
            val intent = createProcessTextIntentForResolveInfo(resolveInfo, editable)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
            context.startActivity(intent)
        }

    private fun createProcessTextIntent(): Intent {
        return Intent().setAction(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
    }

    fun createProcessTextIntentForResolveInfo(info: ResolveInfo, editable: Boolean): Intent {
        return createProcessTextIntent()
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, editable)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
    }

    fun queryProcessTextActivities(context: Context): List<ResolveInfo> {
        return processTextActivitiesQuery(context)
    }

    private fun ResolveInfo.hasPermission(context: Context): Boolean {
        return context.packageName.equals(activityInfo.packageName) ||
                activityInfo.hasPermission(context)
    }

    private fun ActivityInfo.hasPermission(context: Context): Boolean {
        return exported &&
                (permission == null ||
                        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
    }
}
