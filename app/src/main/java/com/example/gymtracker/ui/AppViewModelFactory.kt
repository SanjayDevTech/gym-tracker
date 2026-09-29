package com.example.gymtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Lightweight factory that constructs ViewModels by class. Each screen wraps this in a tiny
 * helper that knows which constructor args to pass — keeps wiring in one place.
 */
class AppViewModelFactory(
    private val builders: Map<Class<out ViewModel>, () -> ViewModel>,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val build = builders[modelClass]
            ?: error("No ViewModel builder registered for ${modelClass.name}")
        return build() as T
    }
}
