package korablique.recipecalculator.model

import korablique.recipecalculator.database.room.IngredientEntity

data class Ingredient(
        val id: Long,
        val foodstuff: Foodstuff,
        val weight: Float,
        val comment: String) {
    companion object {
        fun from(entity: IngredientEntity, foodstuff: Foodstuff): Ingredient {
            return Ingredient(entity.id, foodstuff, entity.ingredientWeight, entity.comment)
        }
        fun new(foodstuff: Foodstuff, weight: Float, comment: String): Ingredient {
            return Ingredient(-1, foodstuff, weight, comment)
        }
    }
}
