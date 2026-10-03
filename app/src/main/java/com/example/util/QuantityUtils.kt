package com.example.util

import kotlin.math.roundToInt

enum class UnitCategory {
    WEIGHT,   // kg, g, gram
    LIQUID,   // L, litre, ml
    DOZEN,    // dozen, pcs
    COUNT     // pc, packet, box, bottle, etc.
}

data class PresetChip(
    val label: String,
    val qtyInBaseUnit: Double
)

object QuantityUtils {

    fun getCategory(unit: String): UnitCategory {
        val u = unit.trim().lowercase()
        return when {
            u in listOf("kg", "kilo", "kilogram", "g", "gm", "gram", "grams") -> UnitCategory.WEIGHT
            u in listOf("l", "litre", "liter", "ltr", "ml", "millilitre") -> UnitCategory.LIQUID
            u in listOf("dozen", "doz") -> UnitCategory.DOZEN
            else -> UnitCategory.COUNT
        }
    }

    /**
     * Parses raw input text into a quantity in the base unit.
     * Examples:
     * - "500g" or "500 gm" for kg item -> 0.5
     * - "250g" -> 0.25
     * - "50g" -> 0.05
     * - "1.5kg" -> 1.5
     * - "500ml" for litre item -> 0.5
     * - "6 pcs" for dozen item -> 0.5
     * - selectedSubUnit == "g" and text == "500" -> 0.5
     * - selectedSubUnit == "ml" and text == "250" -> 0.25
     */
    fun parseQuantity(
        rawText: String,
        baseUnit: String,
        selectedSubUnit: String? = null
    ): Double {
        val trimmed = rawText.trim().lowercase()
        if (trimmed.isEmpty()) return 0.0

        val category = getCategory(baseUnit)

        // 1. Check explicit suffix in input string
        if (trimmed.endsWith("kg") || trimmed.endsWith("kilo")) {
            val num = trimmed.replace("kg", "").replace("kilo", "").trim().toDoubleOrNull() ?: 0.0
            return num
        }
        if (trimmed.endsWith("gm") || trimmed.endsWith("g") || trimmed.endsWith("gram") || trimmed.endsWith("grams")) {
            val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            return if (category == UnitCategory.WEIGHT) num / 1000.0 else num
        }
        if (trimmed.endsWith("ml")) {
            val num = trimmed.replace("ml", "").trim().toDoubleOrNull() ?: 0.0
            return if (category == UnitCategory.LIQUID) num / 1000.0 else num
        }
        if (trimmed.endsWith("l") || trimmed.endsWith("ltr") || trimmed.endsWith("litre") || trimmed.endsWith("liter")) {
            val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            return num
        }
        if (trimmed.endsWith("pcs") || trimmed.endsWith("pc")) {
            val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            return if (category == UnitCategory.DOZEN) num / 12.0 else num
        }

        // 2. No suffix, check selected sub-unit toggle
        val plainNum = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        return when {
            selectedSubUnit == "g" && category == UnitCategory.WEIGHT -> plainNum / 1000.0
            selectedSubUnit == "ml" && category == UnitCategory.LIQUID -> plainNum / 1000.0
            selectedSubUnit == "pcs" && category == UnitCategory.DOZEN -> plainNum / 12.0
            else -> plainNum
        }
    }

    /**
     * Formats display for shopkeeper & invoice:
     * - 0.5 kg -> "500 g (0.5 kg)"
     * - 0.25 kg -> "250 g (0.25 kg)"
     * - 1.5 kg -> "1.5 kg"
     * - 0.5 L -> "500 ml (0.5 L)"
     * - 0.5 dozen -> "6 pcs (0.5 Dozen)"
     */
    fun formatSmartQuantity(qty: Double, unit: String): String {
        val category = getCategory(unit)
        val formattedBase = if (qty % 1.0 == 0.0) qty.toInt().toString() else "%.3f".format(qty).trimEnd('0').trimEnd('.')

        return when (category) {
            UnitCategory.WEIGHT -> {
                if (qty > 0.0 && qty < 1.0) {
                    val grams = (qty * 1000.0).roundToInt()
                    "$grams g ($formattedBase $unit)"
                } else if (qty >= 1.0 && qty % 1.0 != 0.0) {
                    val kgs = qty.toInt()
                    val remGrams = ((qty - kgs) * 1000.0).roundToInt()
                    "$formattedBase $unit ($kgs kg $remGrams g)"
                } else {
                    "$formattedBase $unit"
                }
            }
            UnitCategory.LIQUID -> {
                if (qty > 0.0 && qty < 1.0) {
                    val ml = (qty * 1000.0).roundToInt()
                    "$ml ml ($formattedBase $unit)"
                } else {
                    "$formattedBase $unit"
                }
            }
            UnitCategory.DOZEN -> {
                if (qty % 1.0 != 0.0) {
                    val pcs = (qty * 12.0).roundToInt()
                    "$pcs pcs ($formattedBase dozen)"
                } else {
                    "$formattedBase dozen"
                }
            }
            UnitCategory.COUNT -> "$formattedBase $unit"
        }
    }

    fun getPresetChips(unit: String): List<PresetChip> {
        return when (getCategory(unit)) {
            UnitCategory.WEIGHT -> listOf(
                PresetChip("100g", 0.1),
                PresetChip("250g", 0.25),
                PresetChip("500g", 0.5),
                PresetChip("1 kg", 1.0),
                PresetChip("2 kg", 2.0),
                PresetChip("5 kg", 5.0)
            )
            UnitCategory.LIQUID -> listOf(
                PresetChip("100ml", 0.1),
                PresetChip("250ml", 0.25),
                PresetChip("500ml", 0.5),
                PresetChip("1 L", 1.0),
                PresetChip("2 L", 2.0),
                PresetChip("5 L", 5.0)
            )
            UnitCategory.DOZEN -> listOf(
                PresetChip("1 pc", 1.0 / 12.0),
                PresetChip("3 pcs", 3.0 / 12.0),
                PresetChip("6 pcs (½)", 0.5),
                PresetChip("1 Doz", 1.0),
                PresetChip("2 Doz", 2.0)
            )
            UnitCategory.COUNT -> listOf(
                PresetChip("1", 1.0),
                PresetChip("2", 2.0),
                PresetChip("5", 5.0),
                PresetChip("10", 10.0),
                PresetChip("20", 20.0)
            )
        }
    }
}
