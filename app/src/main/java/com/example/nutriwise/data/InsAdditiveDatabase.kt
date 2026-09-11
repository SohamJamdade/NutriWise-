package com.example.nutriwise.data

object InsAdditiveDatabase {

    private val additiveMap = mapOf(
        "330" to ("INS 330 (Citric Acid)" to "Acidity Regulator: Used to impart tartness and maintain freshness."),
        "500" to ("INS 500(ii) (Sodium Bicarbonate)" to "Raising Agent: Common baking soda used for aeration and leavening."),
        "503" to ("INS 503(ii) (Ammonium Bicarbonate)" to "Leavening Agent: Creates a light, crisp biscuit texture; evaporates during the baking process."),
        "508" to ("INS 508 (Potassium Chloride)" to "Salt Substitute / Gelling Agent: Used to balance salinity or reduce sodium levels."),
        "627" to ("INS 627 (Disodium Guanylate)" to "Flavour Enhancer: Savoury umami enhancer naturally derived from yeast/starch."),
        "631" to ("INS 631 (Disodium Inosinate)" to "Flavour Enhancer: Synergistic savoury enhancer used alongside guanylate."),
        "471" to ("INS 471 (Mono- and Diglycerides)" to "Emulsifier: Plant fat fractions used to blend oil and water phases uniformly."),
        "472e" to ("INS 472e (DATEM)" to "Emulsifier & Dough Strengthener: Improves dough volume and uniform crumb structure in biscuits."),
        "322" to ("INS 322 (Lecithins)" to "Emulsifier: Natural soy or sunflower extract used to stabilize fat emulsions."),
        "319" to ("INS 319 (TBHQ)" to "Antioxidant: Stabilizer used to prevent oxidation in edible vegetable oils."),
        "150d" to ("INS 150d (Caramel IV)" to "Permitted Natural Colour: Imparts consistent golden-brown bakery coloring."),
        "223" to ("INS 223 (Sodium Metabisulphite)" to "Dough Conditioner: Used to relax gluten for consistent biscuit shaping."),
        "1101" to ("INS 1101(i) (Protease / Flour Improver)" to "Enzyme: Naturally breaks down gluten proteins to refine dough consistency.")
    )

    fun decodeAdditive(rawCodeOrName: String): String {
        val digitsOnly = rawCodeOrName.replace(Regex("[^0-9]"), "")
        val match = additiveMap[digitsOnly]
        return if (match != null) {
            "${match.first}: ${match.second}"
        } else {
            "$rawCodeOrName: Permitted food-grade functional ingredient / additive."
        }
    }
}