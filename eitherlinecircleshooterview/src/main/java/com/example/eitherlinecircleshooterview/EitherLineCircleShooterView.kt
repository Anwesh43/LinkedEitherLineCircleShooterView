package com.example.eitherlinecircleshooterview

import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.app.Activity
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas

val colors : Array<Int> = arrayOf(
    "#f44336",
    "#9C27B0",
    "#01579B",
    "#FFD600",
    "#00C853"
).map {
    Color.parseColor(it)
}.toTypedArray()
val rots : Int = 4
val parts : Int = rots + 3
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f
val sizeFactor : Float = 4.9f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")
val rFactor : Float = 23.2f
val rot : Float = 90f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n

fun Canvas.drawEitherLineCircleShooter(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sc1 : Float = scale.divideScale(0, parts)
    val sc2 : Float = scale.divideScale(1, parts)
    val sc7 : Float = scale.divideScale(6, parts)
    val r : Float = Math.min(w, h) / rFactor
    val upSize : Float = size * (sc1 - sc7)
    var k : Float = 0f
    save()
    translate(w / 2, h / 2)
    for (j in 0..3) {
        val scj : Float = scale.divideScale(2 + j, parts)
        k += rot * scj.divideScale(0, parts)
        val x : Float = (h / 2 - size / 2 + r) * scj.divideScale(1, parts)
        save()
        rotate(k)
        drawCircle(x + size / 2, 0f, r * sc2, paint)
        restore()
    }
    rotate(k)
    drawLine(-upSize / 2, 0f, upSize / 2, 0f, paint)
    restore()
}

fun Canvas.drawELCSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawEitherLineCircleShooter(scale, w, h, paint)
}

class EitherLineCircleShooterView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class ELCSNode(var i : Int, val state : State = State()) {

        private var next : ELCSNode? = null
        private var prev : ELCSNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = ELCSNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawELCSNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : ELCSNode {
            var curr : ELCSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class EitherLineCircleShooter(var i : Int) {

        private var curr : ELCSNode = ELCSNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : EitherLineCircleShooterView) {

        private val animator : Animator = Animator(view)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val elcs : EitherLineCircleShooter = EitherLineCircleShooter(0)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            elcs.draw(canvas, paint)
            animator.animate {
                elcs.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            elcs.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : EitherLineCircleShooterView {
            val view : EitherLineCircleShooterView = EitherLineCircleShooterView(activity)
            activity.setContentView(view)
            return view
        }
    }
}