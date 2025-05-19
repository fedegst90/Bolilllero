package com.fedegst.boliapp

class Randomizer(private val range: IntRange) {
    private var numbers = range.toList().shuffled().toMutableList()

    fun next(): Int {
        if (numbers.isEmpty()) {
            numbers = range.toList().shuffled().toMutableList()
        }
        return numbers.removeAt(0)
    }

    fun reset() {
        numbers = range.toList().shuffled().toMutableList()
    }
}
