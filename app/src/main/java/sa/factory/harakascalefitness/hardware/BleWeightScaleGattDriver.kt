package sa.factory.harakascalefitness.hardware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

data class WeightMeasurementGattRecord(
    val weightKg: Double,
    val userId: Int?,
    val bmi: Double?,
    val heightMeters: Double?,
    val isImperial: Boolean = false
)

data class BodyCompositionGattRecord(
    val bodyFatPercentage: Double,
    val basalMetabolismKcal: Int?,
    val muscleMassKg: Double?,
    val fatFreeMassKg: Double?,
    val bodyWaterMassKg: Double?,
    val impedanceOhms: Double?,
    val userId: Int?
)

data class ScaleTelemetryPacket(
    val sequenceNumber: Int,
    val weightRecord: WeightMeasurementGattRecord,
    val compositionRecord: BodyCompositionGattRecord,
    val isValidCrc: Boolean
)

class BleWeightScaleGattDriver {

    companion object {
        const val GATT_WEIGHT_SCALE_SERVICE: Int = 0x181D
        const val GATT_BODY_COMPOSITION_SERVICE: Int = 0x181B
        const val GATT_CHAR_WEIGHT_MEASUREMENT: Int = 0x2A9D
        const val GATT_CHAR_BODY_COMPOSITION: Int = 0x2A9C

        const val FLAG_WEIGHT_IMPERIAL = 0x01
        const val FLAG_WEIGHT_TIMESTAMP = 0x02
        const val FLAG_WEIGHT_USER_ID = 0x04
        const val FLAG_WEIGHT_BMI_HEIGHT = 0x08

        const val FLAG_COMP_IMPERIAL = 0x0001
        const val FLAG_COMP_TIMESTAMP = 0x0002
        const val FLAG_COMP_USER_ID = 0x0004
        const val FLAG_COMP_BMR = 0x0008
        const val FLAG_COMP_MUSCLE_PERCENT = 0x0010
        const val FLAG_COMP_MUSCLE_MASS = 0x0020
        const val FLAG_COMP_FFM = 0x0040
        const val FLAG_COMP_SOFT_LEAN = 0x0080
        const val FLAG_COMP_WATER_MASS = 0x0100
        const val FLAG_COMP_IMPEDANCE = 0x0200
    }

    fun computeCrc16Ccitt(data: ByteArray, offset: Int, length: Int): Short {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return (crc and 0xFFFF).toShort()
    }

    fun encodeWeightMeasurement(
        weightKg: Double,
        userId: Int = 1,
        bmi: Double? = null,
        heightMeters: Double? = null
    ): ByteArray {
        var flags = FLAG_WEIGHT_USER_ID
        if (bmi != null && heightMeters != null) {
            flags = flags or FLAG_WEIGHT_BMI_HEIGHT
        }

        val size = 1 + 2 + 1 + (if (bmi != null) 4 else 0)
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(flags.toByte())
        // Weight resolution is 0.005 kg
        val rawWeight = (weightKg / 0.005).roundToInt().coerceIn(0, 65535)
        buffer.putShort(rawWeight.toShort())
        buffer.put(userId.toByte())

        if (bmi != null && heightMeters != null) {
            val rawBmi = (bmi * 10.0).roundToInt().toShort()
            val rawHeight = (heightMeters * 1000.0).roundToInt().toShort()
            buffer.putShort(rawBmi)
            buffer.putShort(rawHeight)
        }

        return buffer.array()
    }

    fun parseWeightMeasurement(payload: ByteArray): WeightMeasurementGattRecord {
        if (payload.size < 3) throw IllegalArgumentException("Payload too short for Weight Measurement GATT")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        val flags = buffer.get().toInt() and 0xFF
        val isImperial = (flags and FLAG_WEIGHT_IMPERIAL) != 0
        val rawWeight = buffer.short.toInt() and 0xFFFF

        val weight = if (isImperial) {
            rawWeight * 0.01 // lbs
        } else {
            rawWeight * 0.005 // kg
        }

        if ((flags and FLAG_WEIGHT_TIMESTAMP) != 0 && buffer.remaining() >= 7) {
            // Skip 7 bytes of timestamp
            buffer.position(buffer.position() + 7)
        }

        var userId: Int? = null
        if ((flags and FLAG_WEIGHT_USER_ID) != 0 && buffer.hasRemaining()) {
            userId = buffer.get().toInt() and 0xFF
        }

        var bmi: Double? = null
        var heightM: Double? = null
        if ((flags and FLAG_WEIGHT_BMI_HEIGHT) != 0 && buffer.remaining() >= 4) {
            val rawBmi = buffer.short.toInt() and 0xFFFF
            val rawHeight = buffer.short.toInt() and 0xFFFF
            bmi = rawBmi / 10.0
            heightM = rawHeight / 1000.0
        }

        return WeightMeasurementGattRecord(
            weightKg = (weight * 100.0).roundToInt() / 100.0,
            userId = userId,
            bmi = bmi,
            heightMeters = heightM,
            isImperial = isImperial
        )
    }

    fun encodeBodyComposition(
        fatPct: Double,
        userId: Int = 1,
        bmrKcal: Int? = null,
        muscleMassKg: Double? = null,
        fatFreeMassKg: Double? = null,
        waterMassKg: Double? = null,
        impedanceOhms: Double? = null
    ): ByteArray {
        var flags = FLAG_COMP_USER_ID
        if (bmrKcal != null) flags = flags or FLAG_COMP_BMR
        if (muscleMassKg != null) flags = flags or FLAG_COMP_MUSCLE_MASS
        if (fatFreeMassKg != null) flags = flags or FLAG_COMP_FFM
        if (waterMassKg != null) flags = flags or FLAG_COMP_WATER_MASS
        if (impedanceOhms != null) flags = flags or FLAG_COMP_IMPEDANCE

        val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(flags.toShort())

        // Body fat resolution 0.1%
        val rawFat = (fatPct * 10.0).roundToInt().toShort()
        buffer.putShort(rawFat)
        buffer.put(userId.toByte())

        if (bmrKcal != null) {
            buffer.putShort(bmrKcal.toShort())
        }
        if (muscleMassKg != null) {
            buffer.putShort((muscleMassKg / 0.005).roundToInt().toShort())
        }
        if (fatFreeMassKg != null) {
            buffer.putShort((fatFreeMassKg / 0.005).roundToInt().toShort())
        }
        if (waterMassKg != null) {
            buffer.putShort((waterMassKg / 0.005).roundToInt().toShort())
        }
        if (impedanceOhms != null) {
            buffer.putShort((impedanceOhms * 10.0).roundToInt().toShort())
        }

        val out = ByteArray(buffer.position())
        System.arraycopy(buffer.array(), 0, out, 0, buffer.position())
        return out
    }

    fun parseBodyComposition(payload: ByteArray): BodyCompositionGattRecord {
        if (payload.size < 4) throw IllegalArgumentException("Payload too short for Body Composition GATT")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        val flags = buffer.short.toInt() and 0xFFFF
        val rawFat = buffer.short.toInt() and 0xFFFF
        val fatPct = rawFat / 10.0

        if ((flags and FLAG_COMP_TIMESTAMP) != 0 && buffer.remaining() >= 7) {
            buffer.position(buffer.position() + 7)
        }

        var userId: Int? = null
        if ((flags and FLAG_COMP_USER_ID) != 0 && buffer.hasRemaining()) {
            userId = buffer.get().toInt() and 0xFF
        }

        var bmr: Int? = null
        if ((flags and FLAG_COMP_BMR) != 0 && buffer.remaining() >= 2) {
            bmr = buffer.short.toInt() and 0xFFFF
        }

        var muscleMass: Double? = null
        if ((flags and FLAG_COMP_MUSCLE_MASS) != 0 && buffer.remaining() >= 2) {
            muscleMass = (buffer.short.toInt() and 0xFFFF) * 0.005
        }

        var ffm: Double? = null
        if ((flags and FLAG_COMP_FFM) != 0 && buffer.remaining() >= 2) {
            ffm = (buffer.short.toInt() and 0xFFFF) * 0.005
        }

        if ((flags and FLAG_COMP_SOFT_LEAN) != 0 && buffer.remaining() >= 2) {
            buffer.position(buffer.position() + 2) // skip soft lean mass
        }

        var waterMass: Double? = null
        if ((flags and FLAG_COMP_WATER_MASS) != 0 && buffer.remaining() >= 2) {
            waterMass = (buffer.short.toInt() and 0xFFFF) * 0.005
        }

        var impedance: Double? = null
        if ((flags and FLAG_COMP_IMPEDANCE) != 0 && buffer.remaining() >= 2) {
            impedance = (buffer.short.toInt() and 0xFFFF) / 10.0
        }

        return BodyCompositionGattRecord(
            bodyFatPercentage = fatPct,
            basalMetabolismKcal = bmr,
            muscleMassKg = if (muscleMass != null) (muscleMass * 10.0).roundToInt() / 10.0 else null,
            fatFreeMassKg = if (ffm != null) (ffm * 10.0).roundToInt() / 10.0 else null,
            bodyWaterMassKg = if (waterMass != null) (waterMass * 10.0).roundToInt() / 10.0 else null,
            impedanceOhms = impedance,
            userId = userId
        )
    }

    fun buildFramedPacket(
        sequenceNumber: Int,
        weightKg: Double,
        heightM: Double,
        bmi: Double,
        fatPct: Double,
        bmr: Int,
        impedanceOhm: Double
    ): ByteArray {
        val weightPayload = encodeWeightMeasurement(weightKg, 1, bmi, heightM)
        val compPayload = encodeBodyComposition(fatPct, 1, bmr, null, null, null, impedanceOhm)

        val totalSize = 2 + 2 + 1 + weightPayload.size + compPayload.size + 2
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(0x5343.toShort()) // Magic "SC" (Smart Scale)
        buffer.putShort(sequenceNumber.toShort())
        buffer.put(weightPayload.size.toByte())
        buffer.put(weightPayload)
        buffer.put(compPayload)

        val crc = computeCrc16Ccitt(buffer.array(), 0, totalSize - 2)
        buffer.putShort(crc)

        return buffer.array()
    }

    fun parseFramedPacket(packet: ByteArray): ScaleTelemetryPacket {
        if (packet.size < 7) throw IllegalArgumentException("Scale frame packet too short")
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)

        val magic = buffer.short
        if (magic != 0x5343.toShort()) {
            throw IllegalArgumentException("Invalid scale magic header: 0x${Integer.toHexString(magic.toInt() and 0xFFFF)}")
        }

        val seq = buffer.short.toInt() and 0xFFFF
        val weightSize = buffer.get().toInt() and 0xFF

        val weightBytes = ByteArray(weightSize)
        buffer.get(weightBytes)

        val compSize = packet.size - 2 - 2 - 1 - weightSize - 2
        val compBytes = ByteArray(compSize)
        buffer.get(compBytes)

        val packetCrc = buffer.short
        val computedCrc = computeCrc16Ccitt(packet, 0, packet.size - 2)
        val isCrcValid = packetCrc == computedCrc

        val weightRecord = parseWeightMeasurement(weightBytes)
        val compRecord = parseBodyComposition(compBytes)

        return ScaleTelemetryPacket(
            sequenceNumber = seq,
            weightRecord = weightRecord,
            compositionRecord = compRecord,
            isValidCrc = isCrcValid
        )
    }
}
