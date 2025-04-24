package com.example.howl

const val SMALL_AMOUNT = 0.00001
const val TMAX = 1.0 - SMALL_AMOUNT

val generatorWaveShapes: List<WaveShape> = listOf(
    /*WaveShape(
        name = "Sine-ish",
        points = listOf(
            WavePoint(0.0, 0.5, Math.PI),
            WavePoint(0.25, 1.0, 0.0),
            WavePoint(0.5, 0.5, -Math.PI),
            WavePoint(0.75, 0.0, 0.0)
        ),
        interpolationType = InterpolationType.HERMITE
    ),*/
    WaveShape(
        name = "Sawtooth",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(TMAX, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Triangle",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.5, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Square",
        points = listOf(
            WavePoint(0.0, 1.0, 0.0),
            WavePoint(0.5 - SMALL_AMOUNT, 1.0, 0.0),
            WavePoint(0.5, 0.0, 0.0),
            WavePoint(TMAX, 0.0, 0.0)
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Constant",
        points = listOf(
            WavePoint(0.0, 1.0, 0.0),
            WavePoint(TMAX, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Fangs",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.35, 1.0, 0.0),
            WavePoint(0.5, 0.5, 0.0),
            WavePoint(0.65, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Curvy triangle",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.5, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Curvy fangs",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.35, 1.0, 0.0),
            WavePoint(0.5, 0.5, 0.0),
            WavePoint(0.65, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Curvy trapezium",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.4, 0.95, 0.1),
            WavePoint(0.6, 0.95, -0.1),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Gentle attack",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.75, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Fast attack",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.25, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Faster attack",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.15, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Rising tide",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.1, 0.4, 0.0),
            WavePoint(0.2, 0.2, 0.0),
            WavePoint(0.3, 0.6, 0.0),
            WavePoint(0.4, 0.4, 0.0),
            WavePoint(0.5, 0.8, 0.0),
            WavePoint(0.6, 0.6, 0.0),
            WavePoint(0.7, 1.0, 0.0),
            WavePoint(0.8, 0.8, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Flourish",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.5, 0.8, -0.6),
            WavePoint(0.66, 0.6, 0.3),
            WavePoint(0.86, 1.0, 0.0),
            WavePoint(0.9, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Jelly",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.2, 1.0, 0.0),
            WavePoint(0.3, 0.7, 0.0),
            WavePoint(0.4, 1.0, 0.0),
            WavePoint(0.5, 0.7, 0.0),
            WavePoint(0.6, 1.0, 0.0),
            WavePoint(0.7, 0.7, 0.0),
            WavePoint(0.8, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Tap + slide",
        points = listOf(
            WavePoint(0.0, 1.0, 0.0),
            WavePoint(0.1 - SMALL_AMOUNT, 1.0, 0.0),
            WavePoint(0.1, 0.0, 0.0),
            WavePoint(0.2 - SMALL_AMOUNT, 0.0, 0.0),
            WavePoint(0.2, 1.0, 0.0),
            WavePoint(0.3 - SMALL_AMOUNT, 1.0, 0.0),
            WavePoint(0.3, 0.0, 0.0),
            WavePoint(0.4 - SMALL_AMOUNT, 0.0, 0.0),
            WavePoint(0.4, 1.0, 0.0),
            WavePoint(0.5, 1.0, 0.0),
            WavePoint(TMAX, 0.0, 0.0)
        ),
        interpolationType = InterpolationType.LINEAR
    ),
    WaveShape(
        name = "Double time",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.25, 1.0, 0.0),
            WavePoint(0.5, 0.0, 0.0),
            WavePoint(0.625, 1.0, 0.0),
            WavePoint(0.75, 0.0, 0.0),
            WavePoint(0.875, 1.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Triple trouble",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.10, 0.98, 0.05),
            WavePoint(0.14, 1.0, 0.0),
            WavePoint(0.28, 0.0, 0.0),
            WavePoint(0.38, 0.98, 0.05),
            WavePoint(0.42, 1.0, 0.0),
            WavePoint(0.56, 0.0, 0.0),
            WavePoint(0.66, 0.98, 0.05),
            WavePoint(0.7, 1.0, 0.0),
            WavePoint(0.84, 0.0, 0.0),
        ),
        interpolationType = InterpolationType.HERMITE
    ),
    WaveShape(
        name = "Steps",
        points = listOf(
            WavePoint(0.0, 0.0, 0.0),
            WavePoint(0.2 - SMALL_AMOUNT, 0.0, 0.0),
            WavePoint(0.2, 0.25, 0.0),
            WavePoint(0.4 - SMALL_AMOUNT, 0.25, 0.0),
            WavePoint(0.4, 0.5, 0.0),
            WavePoint(0.6 - SMALL_AMOUNT, 0.5, 0.0),
            WavePoint(0.6, 0.75, 0.0),
            WavePoint(0.8 - SMALL_AMOUNT, 0.75, 0.0),
            WavePoint(0.8, 1.0, 0.0),
            WavePoint(TMAX, 1.0, 0.0)
        ),
        interpolationType = InterpolationType.LINEAR
    ),
)