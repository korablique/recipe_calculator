package korablique.recipecalculator.model

import android.os.Parcel
import android.os.Parcelable
import korablique.recipecalculator.database.room.RecipeEntity
import korablique.recipecalculator.model.proto.RecipeProtos

data class Recipe(
        val id: Long,
        val foodstuff: Foodstuff,
        val ingredients: List<Ingredient>,
        val weight: Float,
        val comment: String) : Parcelable {
    companion object {
        fun from(
                entity: RecipeEntity,
                foodstuff: Foodstuff,
                ingredients: List<Ingredient>): Recipe {
            return Recipe(entity.id, foodstuff, ingredients, entity.ingredientsTotalWeight, entity.comment)
        }

        @JvmStatic
        fun create(foodstuff: Foodstuff,
                   ingredients: List<Ingredient>,
                   weight: Float,
                   comment: String): Recipe {
            return Recipe(-1, foodstuff, ingredients, weight, comment)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Recipe> = object : Parcelable.Creator<Recipe> {
            override fun createFromParcel(input: Parcel): Recipe {
                return Recipe(
                        input.readLong(),
                        input.readParcelable(Foodstuff::class.java.classLoader)!!,
                        readIngredients(input),
                        input.readFloat(),
                        input.readString()!!)
            }
            override fun newArray(size: Int): Array<Recipe> {
                return arrayOf()
            }
        }

        @JvmStatic
        fun fromProto(protoRecipe: RecipeProtos.Recipe): Recipe {
            var id: Long = -1
            if (protoRecipe.hasLocalId()) {
                id = protoRecipe.localId
            }
            return Recipe(
                    id,
                    Foodstuff.fromProto(protoRecipe.foodstuff),
                    protoRecipe.ingredientsList.map { Ingredient.fromProto(it) },
                    protoRecipe.weight,
                    protoRecipe.comment)
        }
    }

    val name: String get() = foodstuff.name
    val isFromDB: Boolean get() = id > 0

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(foodstuff, 0)
        dest.writeParcelableArray(ingredients.toTypedArray(), 0)
        dest.writeFloat(weight)
        dest.writeString(comment)
    }

    fun toProto(): RecipeProtos.Recipe {
        return RecipeProtos.Recipe.newBuilder()
                .setLocalId(id)
                .setFoodstuff(foodstuff.toProto())
                .addAllIngredients(ingredients.map { it.toProto() })
                .setWeight(weight)
                .setComment(comment)
                .build()
    }
}

private fun readIngredients(input: Parcel): List<Ingredient> {
    val ingredientsParcelable =
            input.readParcelableArray(Ingredient::class.java.classLoader)!!
    return ingredientsParcelable.map { it as Ingredient }
}
