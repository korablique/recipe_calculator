package korablique.recipecalculator.model

import korablique.recipecalculator.database.room.RecipeEntity

data class Recipe(
        val id: Long,
        val foodstuff: Foodstuff,
        val ingredients: List<Ingredient>,
        val weight: Float,
        val comment: String) {
    companion object {
        fun from(
                entity: RecipeEntity,
                foodstuff: Foodstuff,
                ingredients: List<Ingredient>): Recipe {
            return Recipe(entity.id, foodstuff, ingredients, entity.ingredientsTotalWeight, entity.comment)
        }
    }
}
