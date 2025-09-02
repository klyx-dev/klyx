package com.klyx.editor.completion

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import io.github.rosemoe.sora.widget.component.CompletionLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class AutoCompletionLayout : CompletionLayout {
    private var listView: ListView? = null
    private var progressBar: ProgressBar? = null
    private var rootView: LinearLayout? = null
    private var editorAutoCompletion: EditorAutoCompletion? = null

    private var enabledAnimation = false

    override fun setEditorCompletion(completion: EditorAutoCompletion) {
        editorAutoCompletion = completion
    }

    override fun setEnabledAnimation(enabledAnimation: Boolean) {
        this.enabledAnimation = enabledAnimation

        if (enabledAnimation) {
            val transition = LayoutTransition()
            transition.enableTransitionType(LayoutTransition.CHANGING)
            transition.enableTransitionType(LayoutTransition.APPEARING)
            transition.enableTransitionType(LayoutTransition.DISAPPEARING)
            transition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
            transition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
            transition.addTransitionListener(object : LayoutTransition.TransitionListener {
                override fun startTransition(
                    transition: LayoutTransition?,
                    container: ViewGroup?,
                    view: View?,
                    transitionType: Int
                ) {
                }

                override fun endTransition(
                    transition: LayoutTransition?,
                    container: ViewGroup?,
                    view: View,
                    transitionType: Int
                ) {
                    if (view !== listView) {
                        return
                    }
                    view.requestLayout()
                }
            })
            rootView!!.setLayoutTransition(transition)
            listView!!.setLayoutTransition(transition)
        } else {
            rootView!!.setLayoutTransition(null)
            listView!!.setLayoutTransition(null)
        }
    }

    override fun inflate(context: Context): View {
        val rootLayout = LinearLayout(context)
        rootView = rootLayout
        listView = ListView(context)
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)

        rootLayout.orientation = LinearLayout.VERTICAL

        setEnabledAnimation(false)

        rootLayout.addView(
            progressBar,
            LinearLayout.LayoutParams(
                -1,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics)
                    .toInt()
            )
        )
        rootLayout.addView(listView, LinearLayout.LayoutParams(-1, -1))

        progressBar!!.isIndeterminate = true
        val progressBarLayoutParams = progressBar!!.layoutParams as LinearLayout.LayoutParams

        progressBarLayoutParams.topMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -8f, context.resources.displayMetrics)
                .toInt()
        progressBarLayoutParams.bottomMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -8f, context.resources.displayMetrics)
                .toInt()
        progressBarLayoutParams.leftMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics)
                .toInt()
        progressBarLayoutParams.rightMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics)
                .toInt()

        val gd = GradientDrawable()
        gd.setCornerRadius(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                context.resources.displayMetrics
            )
        )

        rootLayout.background = gd

        setRootViewOutlineProvider(rootView!!)

        listView!!.setDividerHeight(0)
        setLoading(true)

        listView!!.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                try {
                    println("clicked: $position")
                    editorAutoCompletion!!.select(position)
                } catch (e: Exception) {
                    e.printStackTrace(System.err)
                    Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }


        return rootLayout
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        val gd = GradientDrawable()
        gd.setCornerRadius(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                editorAutoCompletion!!.context.resources.displayMetrics
            )
        )
        gd.setStroke(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                editorAutoCompletion!!.context.resources.displayMetrics
            ).toInt(),
            colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER)
        )
        gd.setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
        rootView!!.background = gd

        setRootViewOutlineProvider(rootView!!)
    }

    override fun setLoading(state: Boolean) {
        progressBar!!.visibility = if (state) View.VISIBLE else View.GONE
    }

    override fun getCompletionList(): ListView {
        return listView!!
    }

    /**
     * Perform motion events
     */
    private fun performScrollList(offset: Int) {
        val adpView = completionList

        val down = SystemClock.uptimeMillis()
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()
    }

    private fun setRootViewOutlineProvider(rootView: View) {
        rootView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        view.context.resources.displayMetrics
                    )
                )
            }
        }
        rootView.setClipToOutline(true)
    }

    override fun ensureListPositionVisible(position: Int, increment: Int) {
        listView!!.post(Runnable {
            while (listView!!.firstVisiblePosition + 1 > position && listView!!.canScrollList(-1)) {
                performScrollList(increment / 2)
            }
            while (listView!!.lastVisiblePosition - 1 < position && listView!!.canScrollList(1)) {
                performScrollList(-increment / 2)
            }
        })
    }
}
