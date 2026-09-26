package com.example.util

data class ProductPhotoPreset(
    val id: String,
    val name: String,
    val category: String,
    val url: String,
    val description: String,
    val keywords: List<String> = emptyList()
)

data class PosterPreset(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val tag: String
)

object AgroImagePresets {

    // Reliable agricultural stock image URLs
    const val DEFAULT_PESTICIDE_BOTTLE = "https://images.unsplash.com/photo-1589923188900-85dae523342b?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_FERTILIZER_BAG = "https://images.unsplash.com/photo-1625246333195-78d9c38ad449?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_SEEDS_PACK = "https://images.unsplash.com/photo-1574943320219-553eb213f72d?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_BIO_STIMULANT = "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_HERBICIDE = "https://images.unsplash.com/photo-1592417817098-8f3d6ef23d06?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_FUNGICIDE = "https://images.unsplash.com/photo-1615811361523-6bd03d7748e7?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_SPRAYER = "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=800&q=80"
    const val DEFAULT_MICRONUTRIENT = "https://images.unsplash.com/photo-1585314062340-f1a5a7c9328d?auto=format&fit=crop&w=800&q=80"

    val PRODUCT_PHOTO_PRESETS = listOf(
        ProductPhotoPreset(
            id = "preset_insecticide_bayer",
            name = "Insecticide Liquid Bottle (Confidor / Regent)",
            category = "Insecticides",
            url = DEFAULT_PESTICIDE_BOTTLE,
            description = "Systemic insecticide formulation for sucking and chewing pests",
            keywords = listOf("confidor", "imidacloprid", "regent", "fipronil", "insecticide", "pest", "bayer", "syngenta", "coragen")
        ),
        ProductPhotoPreset(
            id = "preset_fungicide_saaf",
            name = "Broad Spectrum Fungicide (Saaf / Nativo)",
            category = "Fungicides",
            url = DEFAULT_FUNGICIDE,
            description = "Contact & systemic fungicide powder/liquid for blights & rot",
            keywords = listOf("saaf", "nativo", "amistar", "mancozeb", "carbendazim", "fungicide", "fungus", "blight", "upl")
        ),
        ProductPhotoPreset(
            id = "preset_herbicide_roundup",
            name = "Weedicide & Herbicide Canister (Roundup / Nominee)",
            category = "Herbicides",
            url = DEFAULT_HERBICIDE,
            description = "Selective and non-selective weed control solution",
            keywords = listOf("roundup", "glyphosate", "nominee", "bispyribac", "weed", "herbicide", "kharpatwar", "pendimethalin")
        ),
        ProductPhotoPreset(
            id = "preset_fertilizer_nano",
            name = "Nano Fertilizer & Soil Nutrient Pack",
            category = "Fertilizers",
            url = DEFAULT_FERTILIZER_BAG,
            description = "High efficiency water soluble NPK and micronutrient pack",
            keywords = listOf("fertilizer", "urea", "dap", "npk", "iffco", "potash", "zinc", "boron", "coromandel")
        ),
        ProductPhotoPreset(
            id = "preset_seeds_hybrid",
            name = "Certified High-Yield Hybrid Seeds",
            category = "Seeds",
            url = DEFAULT_SEEDS_PACK,
            description = "Germination tested seeds with disease tolerance",
            keywords = listOf("seed", "seeds", "hybrid", "mahyco", "pioneer", "cotton", "wheat", "paddy", "mustard", "bajra")
        ),
        ProductPhotoPreset(
            id = "preset_bio_tonic",
            name = "Bio-Stimulant & Plant Growth Tonic",
            category = "Bio-Nutrients",
            url = DEFAULT_BIO_STIMULANT,
            description = "Organic seaweed and amino acids growth promoter",
            keywords = listOf("bio", "tonic", "stimulant", "growth", "promoter", "seaweed", "amino", "humic", "micronutrient")
        ),
        ProductPhotoPreset(
            id = "preset_sprayer_equipment",
            name = "Battery & Manual Knapsack Sprayer",
            category = "Equipment",
            url = DEFAULT_SPRAYER,
            description = "Agricultural battery and hand operated sprayers and nozzles",
            keywords = listOf("sprayer", "nozzle", "pump", "battery", "tank", "equipment", "knapsack")
        ),
        ProductPhotoPreset(
            id = "preset_crop_booster",
            name = "Flowering & Fruit Setting Booster",
            category = "Bio-Nutrients",
            url = DEFAULT_MICRONUTRIENT,
            description = "Calcium, Boron and Gibberellic acid plant booster",
            keywords = listOf("booster", "flower", "fruit", "calcium", "boron", "zinc", "sulphur", "micronutrient")
        )
    )

    val POSTER_PRESETS = listOf(
        PosterPreset(
            id = "poster_monsoon_1",
            title = "Monsoon Special Crop Protection Offer",
            description = "Get extra 10% cash discount + free freight on orders above ₹50,000 this season.",
            url = "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=1200&q=80",
            tag = "Seasonal Scheme"
        ),
        PosterPreset(
            id = "poster_seeds_1",
            title = "Kharif Hybrid Seeds Carnival",
            description = "Buy 10 packs of certified Mahyco or Syngenta hybrid seeds and get 1 pack complimentary!",
            url = "https://images.unsplash.com/photo-1628352081506-83c43123ed6d?auto=format&fit=crop&w=1200&q=80",
            tag = "Seeds Festival"
        ),
        PosterPreset(
            id = "poster_fertilizer_1",
            title = "Nano Urea & DAP Pre-Booking Bonanza",
            description = "Guaranteed priority allocation and zero price escalation on early booking for next month.",
            url = "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=1200&q=80",
            tag = "Fertilizer Discount"
        ),
        PosterPreset(
            id = "poster_bio_1",
            title = "Bio-Stimulant Cashback Drive",
            description = "Flat ₹500 instant ledger credit for every 20 bottles of organic growth booster ordered.",
            url = "https://images.unsplash.com/photo-1585314062340-f1a5a7c9328d?auto=format&fit=crop&w=1200&q=80",
            tag = "Cashback"
        ),
        PosterPreset(
            id = "poster_early_bird",
            title = "Distributor Early Bird Payment Reward",
            description = "Clear outstanding balance within 3 days of invoice and get 1.5% instant cash discount!",
            url = "https://images.unsplash.com/photo-1574943320219-553eb213f72d?auto=format&fit=crop&w=1200&q=80",
            tag = "Payment Incentive"
        )
    )

    /**
     * Finds a reliable product photo based on name, company, category, or active ingredient.
     * Returns null if no reliable keyword match is found, allowing Admin to choose manually.
     */
    fun findRelevantProductImage(
        itemName: String = "",
        company: String = "",
        category: String = ""
    ): String? {
        val query = "$itemName $company $category".lowercase().trim()
        if (query.isBlank()) return null

        // Direct matching against presets keywords
        for (preset in PRODUCT_PHOTO_PRESETS) {
            if (preset.keywords.any { query.contains(it) }) {
                return preset.url
            }
        }

        // Category-based fallback if meaningful
        val catLower = category.lowercase().trim()
        return when {
            catLower.contains("insect") -> DEFAULT_PESTICIDE_BOTTLE
            catLower.contains("fungi") -> DEFAULT_FUNGICIDE
            catLower.contains("herb") || catLower.contains("weed") -> DEFAULT_HERBICIDE
            catLower.contains("fert") || catLower.contains("nutrient") -> DEFAULT_FERTILIZER_BAG
            catLower.contains("seed") -> DEFAULT_SEEDS_PACK
            catLower.contains("bio") -> DEFAULT_BIO_STIMULANT
            catLower.contains("spray") || catLower.contains("equipment") -> DEFAULT_SPRAYER
            else -> null // Keep blank if no reliable match
        }
    }

    /**
     * Search agricultural photo presets matching query
     */
    fun searchProductImages(query: String = ""): List<ProductPhotoPreset> {
        val clean = query.lowercase().trim()
        if (clean.isBlank()) return PRODUCT_PHOTO_PRESETS
        val matched = PRODUCT_PHOTO_PRESETS.filter { preset ->
            preset.name.lowercase().contains(clean) ||
            preset.category.lowercase().contains(clean) ||
            preset.keywords.any { it.contains(clean) || clean.contains(it) }
        }
        return if (matched.isNotEmpty()) matched else PRODUCT_PHOTO_PRESETS
    }

    fun getAutomaticProductPhoto(category: String, itemName: String): String {
        return findRelevantProductImage(itemName = itemName, category = category) ?: DEFAULT_PESTICIDE_BOTTLE
    }

    fun findAutomaticProductPhoto(
        itemName: String = "",
        company: String = "",
        activeIngredient: String = "",
        itemCode: String = "",
        category: String = ""
    ): String {
        return findRelevantProductImage(
            itemName = "$itemName $activeIngredient",
            company = company,
            category = category
        ) ?: ""
    }

    fun getAutomaticPosterPhoto(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("seed") -> POSTER_PRESETS[1].url
            t.contains("fertilizer") || t.contains("urea") -> POSTER_PRESETS[2].url
            t.contains("bio") || t.contains("cashback") -> POSTER_PRESETS[3].url
            t.contains("payment") || t.contains("clear") || t.contains("due") -> POSTER_PRESETS[4].url
            else -> POSTER_PRESETS[0].url
        }
    }
}
