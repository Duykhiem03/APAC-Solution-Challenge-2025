package com.example.childsafe.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.domain.model.navigation.Route
import com.example.childsafe.domain.usecase.navigation.GetRouteUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the navigation screen that handles fetching and displaying routes
 * between two locations.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val getRouteUseCase: GetRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState

    /**
     * Fetches a route between the specified origin and destination.
     */
    fun getRoute(origin: LatLng, destination: LatLng, showAlternatives: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                getRouteUseCase(
                    origin = origin,
                    destination = destination,
                    alternativesRequested = showAlternatives
                ).collect { result ->
                    result.fold(
                        onSuccess = { routes ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    routes = routes,
                                    selectedRoute = routes.firstOrNull(),
                                    origin = origin,
                                    destination = destination
                                )
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "Error fetching route")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to get route"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in getRoute flow")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    /**
     * Selects a route from the available routes.
     */
    fun selectRoute(route: Route) {
        _uiState.update { it.copy(selectedRoute = route) }
    }

    /**
     * Clears the current navigation state.
     */
    fun clearNavigation() {
        _uiState.update {
            NavigationUiState()
        }
    }
}

/**
 * UI state for the navigation screen.
 */
data class NavigationUiState(
    val isLoading: Boolean = false,
    val routes: List<Route> = emptyList(),
    val selectedRoute: Route? = null,
    val origin: LatLng? = null,
    val destination: LatLng? = null,
    val error: String? = null
)