package com.example.nutriwise.domain

enum class ProductCategory {
    CHIPS_AND_NAMKEEN,
    BISCUITS_AND_COOKIES,
    BREAKFAST_CEREAL,
    NOODLES_AND_PASTA,
    SOFT_DRINKS_AND_JUICES,
    CHOCOLATE_AND_CANDY,
    BREAD_AND_BAKERY,
    SAUCES_AND_SPREADS,
    DAIRY_AND_YOGURT,
    ENERGY_AND_PROTEIN_BARS,
    GENERIC_PACKAGED_FOOD
}

object CategoryDatabase {

    fun detectCategory(text: String): ProductCategory {
        val lower = text.lowercase()
        return when {
            listOf("chip", "crisp", "kurkure", "bhujia", "sev", "namkeen", "wafers", "nachos").any { lower.contains(it) } ->
                ProductCategory.CHIPS_AND_NAMKEEN

            listOf("biscuit", "cookie", "rusk", "cream biscuit", "wafer", "oreo", "bourbon").any { lower.contains(it) } ->
                ProductCategory.BISCUITS_AND_COOKIES

            listOf("cereal", "muesli", "granola", "corn flakes", "chocos", "oats").any { lower.contains(it) } ->
                ProductCategory.BREAKFAST_CEREAL

            listOf("noodle", "ramen", "pasta", "maggi", "macaroni", "spaghetti").any { lower.contains(it) } ->
                ProductCategory.NOODLES_AND_PASTA

            listOf("soda", "cola", "juice", "beverage", "carbonated", "energy drink", "squash").any { lower.contains(it) } ->
                ProductCategory.SOFT_DRINKS_AND_JUICES

            listOf("chocolate", "cocoa", "toffee", "candy", "gummy").any { lower.contains(it) } ->
                ProductCategory.CHOCOLATE_AND_CANDY

            listOf("bread", "bun", "croissant", "cake", "muffin").any { lower.contains(it) } ->
                ProductCategory.BREAD_AND_BAKERY

            listOf("ketchup", "sauce", "mayonnaise", "spread", "jam", "chutney").any { lower.contains(it) } ->
                ProductCategory.SAUCES_AND_SPREADS

            listOf("yogurt", "curd", "paneer", "cheese", "milk", "lassi").any { lower.contains(it) } ->
                ProductCategory.DAIRY_AND_YOGURT

            listOf("protein bar", "granola bar", "energy bar").any { lower.contains(it) } ->
                ProductCategory.ENERGY_AND_PROTEIN_BARS

            else -> ProductCategory.GENERIC_PACKAGED_FOOD
        }
    }

    fun getSmartAlternatives(
        category: ProductCategory,
        hasDiabetes: Boolean = false,
        hasHighBp: Boolean = false
    ): List<HealthAlternative> {
        return when (category) {
            ProductCategory.CHIPS_AND_NAMKEEN -> listOf(
                HealthAlternative(
                    name = "Roasted Makhana (Fox Nuts)",
                    estimatedPrice = "₹80 - ₹140",
                    scoreOutOf100 = 88,
                    whyBetterThanScanned = "0% palm oil, baked crunch with low sodium and zero INS 627/631 enhancers.",
                    cleanSearchQuery = "Roasted Makhana",
                    reason = "Zero palm oil, high fiber, low glycemic index, baked instead of deep fried."
                ),
                HealthAlternative(
                    name = "TagZ Popped Potato Chips",
                    estimatedPrice = "₹60 - ₹90",
                    scoreOutOf100 = 82,
                    whyBetterThanScanned = "Popped with heat and pressure rather than deep-fried in industrial palmolein, cutting fat by 50%.",
                    cleanSearchQuery = "TagZ Popped Chips",
                    reason = "High natural plant protein, low carb spike, high satiety."
                ),
                HealthAlternative(
                    name = "Vacuum-Fried Beetroot & Veggie Chips",
                    estimatedPrice = "₹90 - ₹150",
                    scoreOutOf100 = 79,
                    whyBetterThanScanned = "Fried at low temperatures to retain fiber and nutrients without generating trans-fats.",
                    cleanSearchQuery = "Vacuum Fried Veggie Chips",
                    reason = "Significantly lower saturated fat, no trans fats."
                )
            )

            ProductCategory.BISCUITS_AND_COOKIES -> listOf(
                HealthAlternative(
                    name = "The Whole Truth 100% Sourdough Crackers",
                    estimatedPrice = "₹99 - ₹140",
                    scoreOutOf100 = 90,
                    whyBetterThanScanned = "Zero refined flour (maida), zero artificial emulsifiers, zero refined sugar.",
                    cleanSearchQuery = "The Whole Truth Crackers",
                    reason = "Made with whole millets, lower refined sugar, high soluble fiber."
                ),
                HealthAlternative(
                    name = "Slurrp Farm Ragi & Oat Biscuits",
                    estimatedPrice = "₹45 - ₹90",
                    scoreOutOf100 = 85,
                    whyBetterThanScanned = "Sweetened naturally with jaggery/butter instead of invert sugar syrup and palm fat.",
                    cleanSearchQuery = "Slurrp Farm Biscuits",
                    reason = "Zero refined white sugar, rich in healthy fats and magnesium."
                )
            )

            ProductCategory.BREAKFAST_CEREAL -> listOf(
                HealthAlternative(
                    name = "Yogabar No Added Sugar Muesli",
                    estimatedPrice = "₹220 - ₹350",
                    scoreOutOf100 = 89,
                    whyBetterThanScanned = "Whole rolled oats and nuts with zero added maltodextrin or corn syrup.",
                    cleanSearchQuery = "Yogabar No Sugar Muesli",
                    reason = "High fiber, whole grains, no glucose syrup or artificial colors."
                ),
                HealthAlternative(
                    name = "True Elements Rolled Oats",
                    estimatedPrice = "₹150 - ₹250",
                    scoreOutOf100 = 92,
                    whyBetterThanScanned = "100% whole grain beta-glucan fiber that stabilizes blood sugar.",
                    cleanSearchQuery = "True Elements Rolled Oats",
                    reason = "Complex carbs, steady energy release without sugar crashes."
                )
            )

            ProductCategory.NOODLES_AND_PASTA -> listOf(
                HealthAlternative(
                    name = "WickedGud 100% Millet Hakka Noodles",
                    estimatedPrice = "₹80 - ₹130",
                    scoreOutOf100 = 84,
                    whyBetterThanScanned = "Air-dried rather than flash-fried in palmolein, made with ragi and lentils.",
                    cleanSearchQuery = "WickedGud Millet Noodles",
                    reason = "Not flash-fried in palm oil, double the dietary fiber."
                ),
                HealthAlternative(
                    name = "DiSano 100% Durum Wheat Fusilli Pasta",
                    estimatedPrice = "₹85 - ₹150",
                    scoreOutOf100 = 86,
                    whyBetterThanScanned = "Zero refined maida, higher natural protein content and low glycemic index.",
                    cleanSearchQuery = "DiSano Durum Wheat Pasta",
                    reason = "Higher protein content, no refined flour (maida), lower sodium seasoning."
                )
            )

            ProductCategory.SOFT_DRINKS_AND_JUICES -> listOf(
                HealthAlternative(
                    name = "Raw Pressery Tender Coconut Water",
                    estimatedPrice = "₹60 - ₹80",
                    scoreOutOf100 = 92,
                    whyBetterThanScanned = "Pure potassium electrolytes with 0g artificial sweeteners or acidity regulators.",
                    cleanSearchQuery = "Raw Pressery Coconut Water",
                    reason = "Natural electrolytes, rich in potassium, no artificial sweeteners."
                ),
                HealthAlternative(
                    name = "Sepoy & Co Plain Sparkling Water",
                    estimatedPrice = "₹70 - ₹95",
                    scoreOutOf100 = 95,
                    whyBetterThanScanned = "Zero sugar, zero calories, zero phosphoric acid.",
                    cleanSearchQuery = "Sepoy Plain Sparkling Water",
                    reason = "0g sugar, 0 calories, completely hydrates without glucose spikes."
                )
            )

            ProductCategory.CHOCOLATE_AND_CANDY -> listOf(
                HealthAlternative(
                    name = "Amul 75% Dark Chocolate",
                    estimatedPrice = "₹120 - ₹160",
                    scoreOutOf100 = 84,
                    whyBetterThanScanned = "Rich in cocoa flavonoids, contains a fraction of the sugar found in standard milk chocolate.",
                    cleanSearchQuery = "Amul Dark Chocolate 75",
                    reason = "High antioxidants (flavonoids), 70% less sugar than milk chocolate."
                ),
                HealthAlternative(
                    name = "The Whole Truth Dark Chocolate Bar",
                    estimatedPrice = "₹160 - ₹200",
                    scoreOutOf100 = 88,
                    whyBetterThanScanned = "Only 2 ingredients: cocoa and dates. Zero refined sugar, zero soy lecithin.",
                    cleanSearchQuery = "The Whole Truth Dark Chocolate",
                    reason = "Natural sweetness with fiber, potassium, and zero refined sugars."
                )
            )

            ProductCategory.SAUCES_AND_SPREADS -> listOf(
                HealthAlternative(
                    name = "Pintola All Natural Peanut Butter (Crunchy)",
                    estimatedPrice = "₹160 - ₹260",
                    scoreOutOf100 = 91,
                    whyBetterThanScanned = "100% roasted peanuts. Zero hydrogenated oils, zero palm oil, 0g added sugar.",
                    cleanSearchQuery = "Pintola Natural Peanut Butter",
                    reason = "Pure protein and healthy unsaturated fats, zero palm oil."
                )
            )

            ProductCategory.BREAD_AND_BAKERY,
            ProductCategory.DAIRY_AND_YOGURT,
            ProductCategory.ENERGY_AND_PROTEIN_BARS,
            ProductCategory.GENERIC_PACKAGED_FOOD -> listOf(
                HealthAlternative(
                    name = "The Whole Truth Protein Bar",
                    estimatedPrice = "₹100 - ₹140",
                    scoreOutOf100 = 88,
                    whyBetterThanScanned = "Transparent ingredients sweetened solely with dates, no artificial sweeteners or emulsifiers.",
                    cleanSearchQuery = "The Whole Truth Protein Bar",
                    reason = "Look for products with fewer than 5 ingredients and zero artificial additives."
                ),
                HealthAlternative(
                    name = "Happilo Premium Roasted Almonds & Cashews",
                    estimatedPrice = "₹150 - ₹280",
                    scoreOutOf100 = 93,
                    whyBetterThanScanned = "Whole natural nuts offering healthy fats, protein, and zero chemical preservatives.",
                    cleanSearchQuery = "Happilo Roasted Almonds",
                    reason = "Rich in healthy fats, zero palm oil, high protein and fiber."
                )
            )
        }
    }
}