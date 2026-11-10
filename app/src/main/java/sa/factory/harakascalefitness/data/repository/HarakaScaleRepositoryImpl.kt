package sa.factory.harakascalefitness.data.repository

import sa.factory.harakascalefitness.domain.repository.HarakaScaleRepository
import sa.factory.harakascalefitness.hardware.HardwareSensorDriver
import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder
import sa.factory.harakascalefitness.model.HarakaScaleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HarakaScaleRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : HarakaScaleRepository {

    private val _catalog = MutableStateFlow<List<HarakaScaleItem>>(listOf(
        HarakaScaleItem("1", "تحليل تكوين الجسم الذكي", "قياسات دقيقة عبر ميزان BLE (GATT 0x181D) مع تحليل هندسي لتوازن الأحمال", 150.0, "معتمد", "تحليل الأداء البدني"),
        HarakaScaleItem("2", "مراقبة الإجهاد الميكانيكي", "ربط حزام معدل ضربات القلب مع تقييم الإجهاد الإنشائي في مواقع العمل", 280.0, "فوري", "تقييم السلامة الإنشائية"),
        HarakaScaleItem("3", "تدريب لياقة ميداني", "جلسات تدريبية باستخدام بيانات التيليمتري الحية لتحسين الأداء البدني والعملي", 95.0, "جديد", "تقارير ميكانيكية حية")
    ))
    override val catalogFlow: Flow<List<HarakaScaleItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<HarakaScaleItem>>(emptyList())
    override val activeCart: StateFlow<List<HarakaScaleItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<HarakaScaleTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            HarakaScaleTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: HarakaScaleItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: HarakaScaleItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): HarakaScaleOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = HarakaScaleOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
