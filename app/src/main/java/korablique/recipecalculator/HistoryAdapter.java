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
    private float calorieRate;
    private float proteinRate;
    private float fatRate;
    private float carbRate;

    public HistoryAdapter(
            Observer observer,
            float calorieRate,
            float proteinRate,
            float fatRate,
            float carbRate) {
        this.observer = observer;
        this.calorieRate = calorieRate;
        this.proteinRate = proteinRate;
        this.fatRate = fatRate;
        this.carbRate = carbRate;
    }

    public interface Data {}

    public class FoodstuffData implements Data {
        private HistoryEntry historyEntry;

        public FoodstuffData(HistoryEntry historyEntry) {
            this.historyEntry = historyEntry;
        }

        public HistoryEntry getHistoryEntry() {
            return historyEntry;
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
            final Foodstuff foodstuff = ((FoodstuffData) getItem(position)).getHistoryEntry().getFoodstuff();
            double weight = foodstuff.getWeight();
            setTextViewText(item, R.id.name, foodstuff.getName());
            setTextViewText(item, R.id.weight, foodstuff.getWeight());
            setTextViewText(item, R.id.protein, foodstuff.getProtein() * weight * 0.01);
            setTextViewText(item, R.id.fats, foodstuff.getFats() * weight * 0.01);
            setTextViewText(item, R.id.carbs, foodstuff.getCarbs() * weight * 0.01);
            setTextViewText(item, R.id.calories, foodstuff.getCalories() * weight * 0.01);
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
            setTextViewText(item, R.id.date, dateString);

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
                Foodstuff foodstuff = foodstuffData.getHistoryEntry().getFoodstuff();
                double weight = foodstuff.getWeight();
                ateProtein += foodstuff.getProtein() * weight * 0.01;
                ateFats += foodstuff.getFats() * weight * 0.01;
                ateCarbs += foodstuff.getCarbs() * weight * 0.01;
                ateCalories += foodstuff.getCalories() * weight * 0.01;
            }
            // изменить числа в прогресс барах
            LinearLayout nutritionLayout = ((ProgressViewHolder) holder).getItem();
            setProgressBarValue(nutritionLayout.findViewById(R.id.protein_progress), ateProtein, proteinRate);
            setProgressBarValue(nutritionLayout.findViewById(R.id.fat_progress), ateFats, fatRate);
            setProgressBarValue(nutritionLayout.findViewById(R.id.carbs_progress), ateCarbs, carbRate);
            setProgressBarValue(nutritionLayout.findViewById(R.id.calories_progress), ateCalories, calorieRate);
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

    public void addItem(HistoryEntry historyEntry) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        String newDateString = dateFormat.format(historyEntry.getTime());
        boolean isRequiredDateFound = false;
        for (int index = 0; index < data.size(); index++) {
            if (data.get(index) instanceof DateData) {
                String dateString = dateFormat.format(((DateData) data.get(index)).getDate());
                // если нужная дата найдена
                if (newDateString.equals(dateString)) {
                    data.add(index + 1, new FoodstuffData(historyEntry));
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
                data.add(new DateData(historyEntry.getTime()));
                notifyItemInserted(data.size() - 1);
                data.add(new FoodstuffData(historyEntry));
                notifyItemInserted(data.size() - 1);
                // обновляем вьюшку с прогрессбарами
                notifyItemChanged(data.size() - 2);
            } else {
            // если есть - найти предыдущую и добавить перед ней дату и продукт
                boolean isPreviousDateFound = false;
                for (int index = 0; index < data.size(); index++) {
                    // если дата по индексу - более ранняя
                    if (data.get(index) instanceof DateData
                            && ((DateData)data.get(index)).getDate().compareTo(historyEntry.getTime()) == -1) {
                        data.add(index, new DateData(historyEntry.getTime()));
                        notifyItemInserted(index);
                        data.add(index + 1, new FoodstuffData(historyEntry));
                        notifyItemInserted(index + 1);
                        // обновляем вьюшку с прогрессбарами
                        notifyItemChanged(index);
                        isPreviousDateFound = true;
                        break;
                    }
                }
                // если более ранней даты нет
                if (!isPreviousDateFound) {
                    data.add(new DateData(historyEntry.getTime()));
                    notifyItemInserted(data.size() - 1);
                    data.add(new FoodstuffData(historyEntry));
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

    public void replaceItem(HistoryEntry newHistoryEntry, int displayedPosition) {
        data.set(displayedPosition, new FoodstuffData(newHistoryEntry));
        notifyItemChanged(displayedPosition);
        // пересчитываем прогрессбары
        notifyItemChanged(displayedPosition - 1);
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }

    private void setProgressBarValue(View layout, double currentValue, double maxValue) {
        ProgressBar progressBar = layout.findViewById(R.id.progress_bar);
        progressBar.setProgress((int) Math.round(currentValue));
        progressBar.setMax((int) Math.round(maxValue));

        Formatter formatter = new Formatter();
        formatter.format("%.1f/%.1f", currentValue, maxValue);
        TextView textView = layout.findViewById(R.id.progress_text_view);
        textView.setText(formatter.toString());
    }
}
