package com.example.util

data class ProductPhotoPreset(
    val id: String,
    val name: String,
    val category: String,
    val url: String,
    val description: String
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

    val PRODUCT_PHOTO_PRESETS = listOf(
        ProductPhotoPreset(
            id = "preset_fungicide_1",
            name = "Fungicide Spray Pouch / Bottle",
            category = "Fungicides",
            url = DEFAULT_FUNGICIDE,
            description = "High quality crop protection fungicide solution"
        ),
        ProductPhotoPreset(
            id = "preset_insecticide_1",
            name = "Systemic Insecticide Bottle",
            category = "Insecticides",
            url = DEFAULT_PESTICIDE_BOTTLE,
            description = "Broad-spectrum pest control formulation"
        ),
        ProductPhotoPreset(
            id = "preset_fertilizer_1",
            name = "Organic & Nano Fertilizer",
            category = "Fertilizers",
            url = DEFAULT_FERTILIZER_BAG,
            description = "Balanced NPK and high-efficiency crop nutrient"
        ),
        ProductPhotoPreset(
            id = "preset_seeds_1",
            name = "Certified Hybrid Seeds Pack",
            category = "Seeds",
            url = DEFAULT_SEEDS_PACK,
            description = "High germination, drought & disease resistant hybrid seeds"
        ),
        ProductPhotoPreset(
            id = "preset_herbicide_1",
            name = "Weedicide & Herbicide Canister",
            category = "Herbicides",
            url = DEFAULT_HERBICIDE,
            description = "Selective post-emergence weed control"
        ),
        ProductPhotoPreset(
            id = "preset_bio_1",
            name = "Bio-Stimulant & Plant Growth Booster",
            category = "Bio-Nutrients",
            url = DEFAULT_BIO_STIMULANT,
            description = "Amino acids and seaweed extract plant tonic"
        )
    )

    val POSTER_PRESETS = listOf(
        PosterPreset(
            id = "poster_monsoon_1",
            title = "Monsoon Special Crop Protection Offer",
            description = "Get extra 10% cash discount + free freight on orders above ₹50,000 this monsoon season.",
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

    fun getAutomaticProductPhoto(category: String, itemName: String): String {
        val cat = category.lowercase()
        val name = itemName.lowercase()
        return when {
            cat.contains("seed") || name.contains("seed") -> DEFAULT_SEEDS_PACK
            cat.contains("fert") || name.contains("urea") || name.contains("dap") || name.contains("npk") -> DEFAULT_FERTILIZER_BAG
            cat.contains("bio") || cat.contains("nutrient") || name.contains("tonic") || name.contains("growth") -> DEFAULT_BIO_STIMULANT
            cat.contains("herb") || name.contains("weed") -> DEFAULT_HERBICIDE
            cat.contains("fungi") || name.contains("saaf") || name.contains("nativo") -> DEFAULT_FUNGICIDE
            else -> DEFAULT_PESTICIDE_BOTTLE
        }
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
