package korablique.recipecalculator;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.ID;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private List<Integer> itemsIDs = new ArrayList<>();
    private SQLiteDatabase db;
    private View.OnClickListener onRowClickListener;

    public MyAdapter(SQLiteDatabase db, View.OnClickListener onRowClickListener) {
        this.db = db;
        this.onRowClickListener = onRowClickListener;
        String[] columns = new String[]{ ID };
        Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int itemID = cursor.getInt(cursor.getColumnIndex(ID));
                itemsIDs.add(itemID);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    @Override
    public MyViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.foodstuff_layout, parent, false);
        //прячем столбик с массой
        View weightColumnName = item.findViewById(R.id.column_name_weight);
        weightColumnName.setVisibility(View.GONE);
        View weightView = item.findViewById(R.id.weight);
        weightView.setVisibility(View.GONE);
        item.setOnClickListener(onRowClickListener);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.getItem().setTag(itemsIDs.get(position));

        TextView nameTextView = (TextView) holder.getItem().findViewById(R.id.name);
        TextView proteinTextView = (TextView) holder.getItem().findViewById(R.id.protein);
        TextView fatsTextView = (TextView) holder.getItem().findViewById(R.id.fats);
        TextView carbsTextView = (TextView) holder.getItem().findViewById(R.id.carbs);
        TextView caloriesTextView = (TextView) holder.getItem().findViewById(R.id.calories);

        String name;
        double protein, fats, carbs, calories;

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ID + " = " + itemsIDs.get(position), null);
        boolean hasCursorData = cursor.moveToFirst();
        if (!hasCursorData) {
            throw new RuntimeException("Cursor has no data");
        }
        name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
        protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
        fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
        carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
        calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));

        nameTextView.setText(name);
        proteinTextView.setText(String.valueOf(protein));
        fatsTextView.setText(String.valueOf(fats));
        carbsTextView.setText(String.valueOf(carbs));
        caloriesTextView.setText(String.valueOf(calories));

        cursor.close();
    }

    @Override
    public int getItemCount() {
        return itemsIDs.size();
    }

    public void deleteItem(int id) {
        itemsIDs.remove(Integer.valueOf(id));
        notifyDataSetChanged();
    }
}
