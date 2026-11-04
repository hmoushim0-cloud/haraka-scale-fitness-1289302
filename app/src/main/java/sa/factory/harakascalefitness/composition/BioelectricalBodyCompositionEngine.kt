package sa.factory.harakascalefitness.composition

import kotlin.math.atan2
import kotlin.math.roundToInt

enum class BiologicalSex {
    MALE, FEMALE
}

enum class AthleticLevel {
    SEDENTARY, MODERATELY_ACTIVE, ELITE_ATHLETE
}

data class BioimpedanceRawMeasurement(
    val resistance50kHzOhm: Double,
    val reactance50kHzOhm: Double,
    val weightKg: Double,
    val heightCm: Double,
    val ageYears: Int,
    val sex: BiologicalSex,
    val activityLevel: AthleticLevel = AthleticLevel.MODERATELY_ACTIVE
)

data class BodyCompositionAnalysisResult(
    val bmi: Double,
    val bmiCategoryArabic: String,
    val bodyFatPercentage: Double,
    val bodyFatMassKg: Double,
    val fatFreeMassKg: Double,
    val totalBodyWaterLiters: Double,
    val intracellularWaterLiters: Double,
    val extracellularWaterLiters: Double,
    val skeletalMuscleMassKg: Double,
    val boneMineralContentKg: Double,
    val visceralFatRating: Int,
    val basalMetabolicRateKcal: Int,
    val phaseAngleDegrees: Double,
    val cellularHealthStatusArabic: String,
    val athleticScoreOutOf100: Int
)

data class MetabolicConditioningPlan(
    val restingHeartRateBpm: Int,
    val maxHeartRateBpm: Int,
    val dailyCaloricExpenditureTdee: Int,
    val recommendedDailyProteinGrams: Double,
    val recommendedWaterIntakeLiters: Double,
    val targetHeartRateFatBurnBpm: Int,
    val targetHeartRateCardioBpm: Int
)

class BioelectricalBodyCompositionEngine {

    fun analyzeBodyComposition(input: BioimpedanceRawMeasurement): BodyCompositionAnalysisResult {
        if (input.weightKg <= 0.0 || input.heightCm <= 0.0 || input.resistance50kHzOhm <= 0.0) {
            throw IllegalArgumentException("Weight, height, and impedance resistance must be positive values")
        }

        // 1. BMI Calculation
        val heightM = input.heightCm / 100.0
        val bmi = input.weightKg / (heightM * heightM)
        val bmiCategory = when {
            bmi < 18.5 -> "نقص في الوزن (كتلة منخفضة)"
            bmi in 18.5..24.9 -> "وزن مثالي وطبيعي (مؤشر صحي ممتاز)"
            bmi in 25.0..29.9 -> "زيادة معتدلة في الوزن"
            bmi in 30.0..34.9 -> "سمنة من الدرجة الأولى"
            else -> "سمنة مفرطة تستوجب تدخلا تغذويا"
        }

        // 2. Phase Angle: theta = atan(Xc / R) * (180 / PI)
        val phaseAngleRad = atan2(input.reactance50kHzOhm, input.resistance50kHzOhm)
        val phaseAngleDeg = phaseAngleRad * (180.0 / Math.PI)
        val cellularHealth = when {
            phaseAngleDeg < 4.5 -> "سلامة غشاء الخلية منخفضة (إجهاد خلوي أو احتباس سوائل)"
            phaseAngleDeg in 4.5..6.0 -> "حالة خلوية مقبولة ضمن المتوسط"
            phaseAngleDeg in 6.0..8.5 -> "سلامة خلوية ونشاط غشائي مثالي (لياقة بدنية ممتازة)"
            else -> "مؤشرات خلوية للرياضيين المحترفين وكتلة عضلية عالية"
        }

        // 3. Total Body Water (TBW) via Kushner & Schoeller (1986) Standard BIA formula
        val heightIndex = (input.heightCm * input.heightCm) / input.resistance50kHzOhm
        var tbw = if (input.sex == BiologicalSex.MALE) {
            (0.396 * heightIndex) + (0.143 * input.weightKg) + 8.4
        } else {
            (0.382 * heightIndex) + (0.105 * input.weightKg) + 8.315
        }
        if (tbw < 10.0) tbw = 10.0
        if (tbw > input.weightKg * 0.8) tbw = input.weightKg * 0.75

        val icw = tbw * 0.62
        val ecw = tbw * 0.38

        // 4. Fat-Free Mass (FFM) = TBW / 0.732
        var ffm = tbw / 0.732
        if (ffm > input.weightKg * 0.95) ffm = input.weightKg * 0.92

        // 5. Body Fat Mass & Percentage
        val fatMass = (input.weightKg - ffm).coerceAtLeast(1.0)
        val fatPct = (fatMass / input.weightKg) * 100.0

        // 6. Skeletal Muscle Mass (SMM) via Janssen Equation
        val sexFactor = if (input.sex == BiologicalSex.MALE) 1.0 else 0.0
        var smm = (0.401 * heightIndex) + (3.825 * sexFactor) - (0.071 * input.ageYears) + 5.102
        if (smm > ffm * 0.8) smm = ffm * 0.75
        if (smm < 5.0) smm = 5.0

        // 7. Bone Mineral Content (BMC) ~ 5% of FFM
        val bmc = ffm * 0.052

        // 8. Visceral Fat Rating (1 to 20 scale)
        val visceralScore = ((fatPct * 0.35) + (input.ageYears * 0.1) + ((bmi - 22.0).coerceAtLeast(0.0) * 0.4)).roundToInt().coerceIn(1, 20)

        // 9. Basal Metabolic Rate (BMR) via Katch-McArdle formula: BMR = 370 + 21.6 * FFM
        val bmr = (370.0 + (21.6 * ffm)).roundToInt()

        // 10. Athletic Composite Score (0 to 100)
        var score = 50.0
        // Muscle mass bonus
        val smmRatio = smm / input.weightKg
        score += (smmRatio - 0.32) * 120.0
        // Fat percentage alignment
        val idealFat = if (input.sex == BiologicalSex.MALE) 15.0 else 22.0
        val fatDelta = kotlin.math.abs(fatPct - idealFat)
        score -= fatDelta * 1.5
        // Phase angle bonus
        score += (phaseAngleDeg - 5.5) * 6.0
        val finalScore = score.roundToInt().coerceIn(20, 100)

        return BodyCompositionAnalysisResult(
            bmi = (bmi * 10.0).roundToInt() / 10.0,
            bmiCategoryArabic = bmiCategory,
            bodyFatPercentage = (fatPct * 10.0).roundToInt() / 10.0,
            bodyFatMassKg = (fatMass * 10.0).roundToInt() / 10.0,
            fatFreeMassKg = (ffm * 10.0).roundToInt() / 10.0,
            totalBodyWaterLiters = (tbw * 10.0).roundToInt() / 10.0,
            intracellularWaterLiters = (icw * 10.0).roundToInt() / 10.0,
            extracellularWaterLiters = (ecw * 10.0).roundToInt() / 10.0,
            skeletalMuscleMassKg = (smm * 10.0).roundToInt() / 10.0,
            boneMineralContentKg = (bmc * 10.0).roundToInt() / 10.0,
            visceralFatRating = visceralScore,
            basalMetabolicRateKcal = bmr,
            phaseAngleDegrees = (phaseAngleDeg * 100.0).roundToInt() / 100.0,
            cellularHealthStatusArabic = cellularHealth,
            athleticScoreOutOf100 = finalScore
        )
    }

    fun generateConditioningPlan(
        analysis: BodyCompositionAnalysisResult,
        input: BioimpedanceRawMeasurement,
        restingHeartRateBpm: Int = 65
    ): MetabolicConditioningPlan {
        val maxHr = 220 - input.ageYears

        // Karvonen Heart Rate Reserve (HRR) Zones
        val hrr = maxHr - restingHeartRateBpm
        val fatBurnHr = (restingHeartRateBpm + (hrr * 0.60)).roundToInt()
        val cardioHr = (restingHeartRateBpm + (hrr * 0.75)).roundToInt()

        // Total Daily Energy Expenditure (TDEE) multiplier
        val activityMultiplier = when (input.activityLevel) {
            AthleticLevel.SEDENTARY -> 1.20
            AthleticLevel.MODERATELY_ACTIVE -> 1.55
            AthleticLevel.ELITE_ATHLETE -> 1.85
        }
        val tdee = (analysis.basalMetabolicRateKcal * activityMultiplier).roundToInt()

        // Recommended protein: 1.6g to 2.2g per kg of FFM based on activity
        val proteinMultiplier = when (input.activityLevel) {
            AthleticLevel.SEDENTARY -> 1.4
            AthleticLevel.MODERATELY_ACTIVE -> 1.8
            AthleticLevel.ELITE_ATHLETE -> 2.2
        }
        val dailyProtein = (analysis.fatFreeMassKg * proteinMultiplier * 10.0).roundToInt() / 10.0

        // Recommended water: 35 mL per kg body weight + active training allowance
        val baseWater = input.weightKg * 0.035
        val extraWater = if (input.activityLevel == AthleticLevel.ELITE_ATHLETE) 1.0 else 0.5
        val totalWater = ((baseWater + extraWater) * 10.0).roundToInt() / 10.0

        return MetabolicConditioningPlan(
            restingHeartRateBpm = restingHeartRateBpm,
            maxHeartRateBpm = maxHr,
            dailyCaloricExpenditureTdee = tdee,
            recommendedDailyProteinGrams = dailyProtein,
            recommendedWaterIntakeLiters = totalWater,
            targetHeartRateFatBurnBpm = fatBurnHr,
            targetHeartRateCardioBpm = cardioHr
        )
    }
}
