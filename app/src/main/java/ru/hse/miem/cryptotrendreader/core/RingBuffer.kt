package ru.hse.miem.cryptotrendreader.core

import timber.log.Timber
import java.util.ArrayList

class RingBuffer<T>(private val capacity: Int) : Iterable<T> {
    private val data = ArrayList<T>(capacity)
    private var start = 0

    fun add(item: T) {
        if (data.size < capacity) {
            data.add(item)
        } else {
            if (capacity > 0) {
                data[start] = item
                start = (start + 1) % capacity
            }
        }
    }

    fun toListOrdered(): List<T> {
        if (capacity == 0) return emptyList()
        return if (data.size < capacity) {
            data.toList()
        } else {
            val list = ArrayList<T>(capacity)
            for (i in 0 until capacity) {
                list.add(data[(start + i) % capacity])
            }
            list
        }
    }

    override fun iterator(): Iterator<T> = toListOrdered().iterator()

    val size get() = data.size
    val isEmpty get() = data.isEmpty()

    fun clear() {
        data.clear()
        start = 0
        Timber.tag("RingBuffer").v("Buffer cleared. Capacity: $capacity")
    }
}