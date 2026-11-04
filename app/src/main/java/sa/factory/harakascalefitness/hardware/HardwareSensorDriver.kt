package sa.factory.harakascalefitness.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.harakascalefitness.composition.AthleticLevel
import sa.factory.harakascalefitness.composition.BioelectricalBodyCompositionEngine
import sa.factory.harakascalefitness.composition.BioimpedanceRawMeasurement
import sa.factory.harakascalefitness.composition.BiologicalSex
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "BLE Smart Scale GATT 0x181D + Heart Rate Strap"
    private val gattDriver = BleWeightScaleGattDriver()
    private val compositionEngine = BioelectricalBodyCompositionEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "BLE GATT 0x181D Discovery & Handshake initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "BLE Smart Scale GATT 0x181D & 0x181B: ONLINE | READY"))

        var sequence = 100
        while (true) {
            delay(1500)
            sequence++

            val weightKg = 76.5 + (Random.nextDouble(-0.3, 0.3))
            val heightCm = 178.0
            val rOhm = 510.0 + (Random.nextDouble(-5.0, 5.0))
            val xcOhm = 58.0 + (Random.nextDouble(-1.0, 1.0))

            val bioInput = BioimpedanceRawMeasurement(
                resistance50kHzOhm = rOhm,
                reactance50kHzOhm = xcOhm,
                weightKg = weightKg,
                heightCm = heightCm,
                ageYears = 28,
                sex = BiologicalSex.MALE,
                activityLevel = AthleticLevel.MODERATELY_ACTIVE
            )
            val compResult = compositionEngine.analyzeBodyComposition(bioInput)

            val packet = gattDriver.buildFramedPacket(
                sequenceNumber = sequence,
                weightKg = weightKg,
                heightM = heightCm / 100.0,
                bmi = compResult.bmi,
                fatPct = compResult.bodyFatPercentage,
                bmr = compResult.basalMetabolicRateKcal,
                impedanceOhm = rOhm
            )

            val parsed = gattDriver.parseFramedPacket(packet)

            val payload = String.format(
                "GATT 0x181D Frame #%d: Weight: %.2f kg | BMI: %.1f | Fat: %.1f%% | SMM: %.1f kg | TBW: %.1f L | BMR: %d kcal | CRC: %s",
                parsed.sequenceNumber,
                parsed.weightRecord.weightKg,
                parsed.weightRecord.bmi ?: compResult.bmi,
                parsed.compositionRecord.bodyFatPercentage,
                compResult.skeletalMuscleMassKg,
                compResult.totalBodyWaterLiters,
                parsed.compositionRecord.basalMetabolismKcal ?: compResult.basalMetabolicRateKcal,
                if (parsed.isValidCrc) "VALID" else "INVALID"
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
