package sa.factory.harakascalefitness

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sa.factory.harakascalefitness.composition.*
import sa.factory.harakascalefitness.hardware.BleWeightScaleGattDriver

class HarakaScaleFitnessFeatureTest {

    private lateinit var compositionEngine: BioelectricalBodyCompositionEngine
    private lateinit var gattDriver: BleWeightScaleGattDriver

    @Before
    fun setUp() {
        compositionEngine = BioelectricalBodyCompositionEngine()
        gattDriver = BleWeightScaleGattDriver()
    }

    @Test
    fun testBiaBodyCompositionMetrics() {
        // Healthy 28-year-old male: 75.0 kg, 178 cm, R=520 Ohm, Xc=60 Ohm
        val input = BioimpedanceRawMeasurement(
            resistance50kHzOhm = 520.0,
            reactance50kHzOhm = 60.0,
            weightKg = 75.0,
            heightCm = 178.0,
            ageYears = 28,
            sex = BiologicalSex.MALE,
            activityLevel = AthleticLevel.MODERATELY_ACTIVE
        )

        val result = compositionEngine.analyzeBodyComposition(input)

        // BMI = 75 / (1.78^2) = 23.67 -> 23.7
        assertEquals(23.7, result.bmi, 0.1)
        assertTrue(result.bmiCategoryArabic.contains("وزن مثالي"))

        // Phase angle = atan(60 / 520) * (180 / PI) = 6.58 degrees
        assertEquals(6.58, result.phaseAngleDegrees, 0.1)
        assertTrue(result.cellularHealthStatusArabic.contains("سلامة خلوية"))

        // Physiological sum checks: FFM + Fat Mass = Total Weight
        val computedTotalWeight = result.fatFreeMassKg + result.bodyFatMassKg
        assertEquals(75.0, computedTotalWeight, 0.2)

        // Total body water should be around 55-65% of body weight for a healthy male
        assertTrue(result.totalBodyWaterLiters in 38.0..52.0)
        assertEquals(result.totalBodyWaterLiters, result.intracellularWaterLiters + result.extracellularWaterLiters, 0.2)

        // Skeletal muscle mass and BMR checks
        assertTrue(result.skeletalMuscleMassKg > 25.0)
        assertTrue(result.basalMetabolicRateKcal in 1500..2000)
        assertTrue(result.athleticScoreOutOf100 in 55..100)
    }

    @Test
    fun testMetabolicConditioningPlan() {
        val input = BioimpedanceRawMeasurement(
            resistance50kHzOhm = 520.0,
            reactance50kHzOhm = 60.0,
            weightKg = 75.0,
            heightCm = 178.0,
            ageYears = 28,
            sex = BiologicalSex.MALE,
            activityLevel = AthleticLevel.MODERATELY_ACTIVE
        )

        val result = compositionEngine.analyzeBodyComposition(input)
        val plan = compositionEngine.generateConditioningPlan(result, input, restingHeartRateBpm = 62)

        // Max HR = 220 - 28 = 192 bpm
        assertEquals(192, plan.maxHeartRateBpm)
        assertEquals(62, plan.restingHeartRateBpm)

        // HRR = 192 - 62 = 130
        // Fat burn = 62 + 130 * 0.60 = 140 bpm
        // Cardio = 62 + 130 * 0.75 = 160 bpm (approx)
        assertTrue(plan.targetHeartRateFatBurnBpm in 138..142)
        assertTrue(plan.targetHeartRateCardioBpm in 158..162)

        // Moderately active TDEE multiplier = 1.55 * BMR
        assertTrue(plan.dailyCaloricExpenditureTdee > result.basalMetabolicRateKcal)
        assertTrue(plan.recommendedDailyProteinGrams > 90.0)
        assertTrue(plan.recommendedWaterIntakeLiters >= 2.8)
    }

    @Test
    fun testFemaleBodyCompositionDifferential() {
        val inputFemale = BioimpedanceRawMeasurement(
            resistance50kHzOhm = 590.0,
            reactance50kHzOhm = 55.0,
            weightKg = 62.0,
            heightCm = 165.0,
            ageYears = 26,
            sex = BiologicalSex.FEMALE,
            activityLevel = AthleticLevel.MODERATELY_ACTIVE
        )

        val result = compositionEngine.analyzeBodyComposition(inputFemale)
        // BMI = 62 / (1.65^2) = 22.77 -> 22.8
        assertEquals(22.8, result.bmi, 0.1)
        assertTrue(result.bodyFatPercentage in 18.0..32.0)
        assertTrue(result.skeletalMuscleMassKg > 18.0)
        assertEquals(62.0, result.fatFreeMassKg + result.bodyFatMassKg, 0.2)
    }

    @Test
    fun testWeightMeasurementGattEncodeAndDecode() {
        val weight = 78.45
        val userId = 3
        val bmi = 24.8
        val heightM = 1.78

        val payload = gattDriver.encodeWeightMeasurement(weight, userId, bmi, heightM)
        val record = gattDriver.parseWeightMeasurement(payload)

        assertEquals(weight, record.weightKg, 0.01)
        assertEquals(userId, record.userId)
        assertNotNull(record.bmi)
        assertEquals(bmi, record.bmi!!, 0.1)
        assertNotNull(record.heightMeters)
        assertEquals(heightM, record.heightMeters!!, 0.005)
        assertFalse(record.isImperial)
    }

    @Test
    fun testBodyCompositionGattEncodeAndDecode() {
        val fatPct = 16.5
        val userId = 2
        val bmr = 1750
        val muscleMass = 34.2
        val ffm = 61.5
        val waterMass = 42.0
        val impedance = 512.5

        val payload = gattDriver.encodeBodyComposition(
            fatPct = fatPct,
            userId = userId,
            bmrKcal = bmr,
            muscleMassKg = muscleMass,
            fatFreeMassKg = ffm,
            waterMassKg = waterMass,
            impedanceOhms = impedance
        )

        val record = gattDriver.parseBodyComposition(payload)
        assertEquals(fatPct, record.bodyFatPercentage, 0.1)
        assertEquals(userId, record.userId)
        assertEquals(bmr, record.basalMetabolismKcal)
        assertNotNull(record.muscleMassKg)
        assertEquals(muscleMass, record.muscleMassKg!!, 0.1)
        assertNotNull(record.fatFreeMassKg)
        assertEquals(ffm, record.fatFreeMassKg!!, 0.1)
        assertNotNull(record.bodyWaterMassKg)
        assertEquals(waterMass, record.bodyWaterMassKg!!, 0.1)
        assertNotNull(record.impedanceOhms)
        assertEquals(impedance, record.impedanceOhms!!, 0.1)
    }

    @Test
    fun testFramedTelemetryPacketAndCrcVerification() {
        val seq = 105
        val weight = 81.20
        val heightM = 1.80
        val bmi = 25.1
        val fatPct = 18.2
        val bmr = 1820
        val impedance = 495.0

        val packet = gattDriver.buildFramedPacket(seq, weight, heightM, bmi, fatPct, bmr, impedance)
        val telemetry = gattDriver.parseFramedPacket(packet)

        assertEquals(seq, telemetry.sequenceNumber)
        assertEquals(weight, telemetry.weightRecord.weightKg, 0.01)
        assertEquals(fatPct, telemetry.compositionRecord.bodyFatPercentage, 0.1)
        assertTrue("CRC-16-CCITT should be verified as valid", telemetry.isValidCrc)

        // Test Corrupted CRC Detection
        val corrupted = packet.copyOf()
        corrupted[10] = (corrupted[10].toInt() xor 0xFF).toByte()
        val corruptTelemetry = gattDriver.parseFramedPacket(corrupted)
        assertFalse("Corrupted frame CRC must fail verification", corruptTelemetry.isValidCrc)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidInputGuards() {
        val invalid = BioimpedanceRawMeasurement(
            resistance50kHzOhm = -10.0,
            reactance50kHzOhm = 50.0,
            weightKg = 70.0,
            heightCm = 175.0,
            ageYears = 25,
            sex = BiologicalSex.MALE
        )
        compositionEngine.analyzeBodyComposition(invalid)
    }
}
