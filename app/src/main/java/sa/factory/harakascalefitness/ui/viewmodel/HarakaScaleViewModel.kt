package sa.factory.harakascalefitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.harakascalefitness.domain.usecase.*
import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder
import sa.factory.harakascalefitness.model.HarakaScaleTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HarakaScaleUiState(
    val catalog: List<HarakaScaleItem> = emptyList(),
    val filteredCatalog: List<HarakaScaleItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<HarakaScaleItem> = emptyList(),
    val telemetry: HarakaScaleTelemetry? = null,
    val lastOrder: HarakaScaleOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class HarakaScaleViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HarakaScaleUiState())
    val uiState: StateFlow<HarakaScaleUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: HarakaScaleItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: HarakaScaleItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
