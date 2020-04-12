package korablique.recipecalculator.model

import android.os.Parcel
import android.os.Parcelable
import korablique.recipecalculator.database.room.IngredientEntity

data class Ingredient(
        val id: Long,
        val foodstuff: Foodstuff,
        val weight: Float,
        val comment: String) : Parcelable {
    companion object {
        fun from(entity: IngredientEntity, foodstuff: Foodstuff): Ingredient {
            return Ingredient(entity.id, foodstuff, entity.ingredientWeight, entity.comment)
        }

        @JvmStatic
        fun create(foodstuff: Foodstuff, weight: Float, comment: String): Ingredient {
            return Ingredient(-1, foodstuff, weight, comment)
        }

        @JvmStatic
        fun create(foodstuff: WeightedFoodstuff, comment: String): Ingredient {
            return Ingredient(-1, foodstuff.withoutWeight(), foodstuff.weight.toFloat(), comment)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Ingredient> = object : Parcelable.Creator<Ingredient> {
            override fun createFromParcel(input: Parcel): Ingredient {
                return Ingredient(
                        input.readLong(),
                        input.readParcelable(Foodstuff::class.java.classLoader)!!,
                        input.readFloat(),
                        input.readString()!!)
            }
            override fun newArray(size: Int): Array<Ingredient> {
                return arrayOf()
            }
        }
    }

    fun toWeightedFoodstuff(): WeightedFoodstuff {
        return WeightedFoodstuff(foodstuff, weight.toDouble())
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(foodstuff, 0)
        dest.writeFloat(weight)
        dest.writeString(comment)
    }
}
