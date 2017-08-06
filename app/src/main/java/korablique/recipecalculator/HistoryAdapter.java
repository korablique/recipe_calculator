package korablique.recipecalculator;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;


public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Observer {
        void onItemClicked(Foodstuff foodstuff, int displayedPosition);
    }
    public static final int ITEM_TYPE_PROGRESS = 0;
    public static final int ITEM_TYPE_FOODSTUFF = 1;
    public List<Data> data = new ArrayList<>();
    private Observer observer;

    public HistoryAdapter(Observer observer) {
        this.observer = observer;
    }

    public interface Data {}

    public class FoodstuffData implements Data {
        private Foodstuff foodstuff;

        public FoodstuffData(Foodstuff foodstuff) {
            this.foodstuff = foodstuff;
        }

        public Foodstuff getFoodstuff() {
            return foodstuff;
        }
    }

    public class DateData implements Data {
        private Date date;

        public DateData(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_FOODSTUFF) {
            LinearLayout foodstuffView = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.foodstuff_layout, parent, false);
            return new FoodstuffViewHolder(foodstuffView);
        } else {
            LinearLayout nutritionProgress = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nutrition_progress_layout, parent, false);
            return new ProgressViewHolder(nutritionProgress);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        int itemType = getItemViewType(position);
        if (itemType == ITEM_TYPE_FOODSTUFF) {
            LinearLayout item = ((FoodstuffViewHolder)holder).getItem();
            final Foodstuff foodstuff = ((FoodstuffData) getItem(position)).getFoodstuff();
            ((TextView) item.findViewById(R.id.name)).setText(foodstuff.getName());
            ((TextView) item.findViewById(R.id.weight)).setText(String.valueOf(foodstuff.getWeight()));
            ((TextView) item.findViewById(R.id.protein)).setText(String.valueOf(foodstuff.getProtein()));
            ((TextView) item.findViewById(R.id.fats)).setText(String.valueOf(foodstuff.getFats()));
            ((TextView) item.findViewById(R.id.carbs)).setText(String.valueOf(foodstuff.getCarbs()));
            ((TextView) item.findViewById(R.id.calories)).setText(String.valueOf(foodstuff.getCalories()));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    observer.onItemClicked(foodstuff, holder.getAdapterPosition());
                }
            });
        } else if (itemType == ITEM_TYPE_PROGRESS) {
            LinearLayout item = ((ProgressViewHolder)holder).getItem();
            Date date = ((DateData) getItem(position)).getDate();
            DateFormat df = DateFormat.getDateInstance();
            String dateString = df.format(date);
            ((TextView) item.findViewById(R.id.date)).setText(dateString);

            // пройтись по всем фудстаффам этого нутришена и пересчитать бжук
            // если после этой вьюшки нет фудстаффов, то в прогресс-барах её будут числа из превью
            ArrayList<FoodstuffData> dailyFood = new ArrayList<>();
            for (int index = position + 1; index < data.size(); index++) {
                if (data.get(index) instanceof FoodstuffData) {
                    dailyFood.add((FoodstuffData) data.get(index));
                } else {
                    break;
                }
            }
            // TODO: 05.08.17 сделать так, чтобы фудстафф отображал бжук на количество съеденного, а не на 100 г
            // умножить бжук на массу съеденного продукта
            // сложить с остальными
            double ateProtein = 0, ateFats = 0, ateCarbs = 0, ateCalories = 0;
            for (FoodstuffData foodstuffData : dailyFood) {
                Foodstuff foodstuff = foodstuffData.getFoodstuff();
                double weight = foodstuff.getWeight();
                ateProtein += foodstuff.getProtein() * weight * 0.01;
                ateFats += foodstuff.getFats() * weight * 0.01;
                ateCarbs += foodstuff.getCarbs() * weight * 0.01;
                ateCalories += foodstuff.getCalories() * weight * 0.01;
            }
            // изменить числа в прогресс барах
            LinearLayout nutritionLayout = ((ProgressViewHolder) holder).getItem();
            ProgressBar proteinProgressBar = (ProgressBar) nutritionLayout.findViewById(R.id.protein_progress)
                    .findViewById(R.id.progress_bar);
            proteinProgressBar.setProgress((int) Math.round(ateProtein));
            proteinProgressBar.setMax(94);

            Formatter proteinFormatter = new Formatter();
            proteinFormatter.format("%.1f", ateProtein);
            TextView proteinTextView = (TextView) nutritionLayout.findViewById(R.id.protein_progress)
                    .findViewById(R.id.progress_text_view);
            proteinTextView.setText(proteinFormatter.toString() + "/" + 94);


            ProgressBar fatsProgressBar = (ProgressBar) nutritionLayout.findViewById(R.id.fat_progress)
                    .findViewById(R.id.progress_bar);
            fatsProgressBar.setProgress((int) Math.round(ateFats));
            fatsProgressBar.setMax(47);

            Formatter fatsFormatter = new Formatter();
            fatsFormatter.format("%.1f", ateFats);
            TextView fatsTextView = (TextView) nutritionLayout.findViewById(R.id.fat_progress)
                    .findViewById(R.id.progress_text_view);
            fatsTextView.setText(fatsFormatter.toString() + "/" + 47);


            ProgressBar carbsProgressBar = (ProgressBar) nutritionLayout.findViewById(R.id.carbs_progress)
                    .findViewById(R.id.progress_bar);
            carbsProgressBar.setProgress((int) Math.round(ateCarbs));
            carbsProgressBar.setMax(238);

            Formatter carbsFormatter = new Formatter();
            carbsFormatter.format("%.1f", ateCarbs);
            TextView carbsTextView = (TextView) nutritionLayout.findViewById(R.id.carbs_progress)
                    .findViewById(R.id.progress_text_view);
            carbsTextView.setText(carbsFormatter.toString() + "/" + 238);


            ProgressBar caloriesProgressBar = (ProgressBar) nutritionLayout.findViewById(R.id.calories_progress)
                    .findViewById(R.id.progress_bar);
            caloriesProgressBar.setProgress((int) Math.round(ateCalories));
            caloriesProgressBar.setMax(1771);

            Formatter caloriesFormatter = new Formatter();
            caloriesFormatter.format("%.1f", ateCalories);
            TextView caloriesTextView = (TextView) nutritionLayout.findViewById(R.id.calories_progress)
                    .findViewById(R.id.progress_text_view);
            caloriesTextView.setText(caloriesFormatter.toString() + "/" + 1771);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position) instanceof DateData) {
            return ITEM_TYPE_PROGRESS;
        } else {
            return ITEM_TYPE_FOODSTUFF;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public Data getItem(int position) {
        return data.get(position);
    }

    public void addItem(Foodstuff foodstuff, Date newDate) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        String newDateString = dateFormat.format(newDate);
        boolean isRequiredDateFound = false;
        for (int index = 0; index < data.size(); index++) {
            if (data.get(index) instanceof DateData) {
                String dateString = dateFormat.format(((DateData) data.get(index)).getDate());
                // если нужная дата найдена
                if (newDateString.equals(dateString)) {
                    data.add(index + 1, new FoodstuffData(foodstuff));
                    notifyItemInserted(index + 1);
                    // обновляем вьюшку с прогрессбарами
                    notifyItemChanged(index);
                    isRequiredDateFound = true;
                    break;
                }
            }
        }
        if (!isRequiredDateFound) {
            // если ещё ни одной даты нет - добавить новую + продукт
            if (data.size() == 0) {
                data.add(new DateData(newDate));
                notifyItemInserted(data.size() - 1);
                data.add(new FoodstuffData(foodstuff));
                notifyItemInserted(data.size() - 1);
                // обновляем вьюшку с прогрессбарами
                notifyItemChanged(data.size() - 2);
            } else {
            // если есть - найти предыдущую и добавить перед ней дату и продукт
                boolean isPreviousDateFound = false;
                for (int index = 0; index < data.size(); index++) {
                    // если дата по индексу - более ранняя
                    if (data.get(index) instanceof DateData && ((DateData)data.get(index)).getDate().compareTo(newDate) == -1) {
                        data.add(index, new DateData(newDate));
                        notifyItemInserted(index);
                        data.add(index + 1, new FoodstuffData(foodstuff));
                        notifyItemInserted(index + 1);
                        // обновляем вьюшку с прогрессбарами
                        notifyItemChanged(index);
                        isPreviousDateFound = true;
                        break;
                    }
                }
                // если более ранней даты нет
                if (!isPreviousDateFound) {
                    data.add(new DateData(newDate));
                    notifyItemInserted(data.size() - 1);
                    data.add(new FoodstuffData(foodstuff));
                    notifyItemInserted(data.size() - 1);
                }
            }
        }
    }

    public void deleteItem(int displayedPosition) {
        /*Foodstuff deleted = filteredFoodstuffs.get(displayedPosition);
        int indexInAllFoodstuffs = allFoodstuffs.indexOf(deleted);
        filteredFoodstuffs.remove(displayedPosition);
        allFoodstuffs.remove(indexInAllFoodstuffs);
        notifyItemRemoved(displayedPosition);
        observer.onItemsCountChanged(allFoodstuffs.size());*/
        int requiredDateIndex = -1;
        // ищем дату, из которой нужно удалить продукт
        for (int index = displayedPosition - 1; index >= 0; index--) {
            if (data.get(index) instanceof DateData) {
                requiredDateIndex = index;
                break;
            }
        }
        int dailyFoodsCount = 0;
        for (int index = requiredDateIndex + 1; index < data.size(); index++) {
            if (data.get(index) instanceof FoodstuffData) {
                dailyFoodsCount += 1;
            } else {
                break;
            }
        }
        // если этим числом был добавлен только один продукт
        if (dailyFoodsCount == 1) {
            // удаляем продукт
            data.remove(displayedPosition);
            notifyItemRemoved(displayedPosition);
            //удаляем нутришн
            data.remove(displayedPosition - 1);
            notifyItemRemoved(displayedPosition - 1);
        } else {
            data.remove(displayedPosition);
            notifyItemRemoved(displayedPosition);
        }
    }

    public void replaceItem(Foodstuff newFoodstuff, int displayedPosition) {
        data.set(displayedPosition, new FoodstuffData(newFoodstuff));
        notifyItemChanged(displayedPosition);
        // пересчитываем прогрессбары
        notifyItemChanged(displayedPosition - 1);
    }
}
