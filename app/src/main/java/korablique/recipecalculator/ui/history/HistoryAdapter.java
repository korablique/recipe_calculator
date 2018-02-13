package korablique.recipecalculator.ui.history;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.ui.MyViewHolder;


public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Observer {
        void onItemClicked(Foodstuff foodstuff, int displayedPosition);
    }
    private static final int ITEM_TYPE_PROGRESS = 0;
    private static final int ITEM_TYPE_FOODSTUFF = 1;
    private Context context;
    private List<Data> data = new ArrayList<>();
    private Observer observer;
    private Rates rates;

    public HistoryAdapter(Context context, Observer observer, Rates rates) {
        this.context = context;
        this.observer = observer;
        this.rates = rates;
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
        private long daySinceBC;

        public DateData(Date date) {
            this.date = date;
            // Работа с датами очень медленная штука, поэтому, зная что нам придётся сравнивать
            // дни между собой, кешируем день заранее.
            this.daySinceBC = calculateDaySinceBC(date);
        }

        public Date getDate() {
            return date;
        }

        public long getDaySinceBC() {
            return daySinceBC;
        }
    }

    /**
     * День от начала нулевого года.
     */
    private long calculateDaySinceBC(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_YEAR) + calendar.get(Calendar.YEAR) * 365;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_FOODSTUFF) {
            LinearLayout foodstuffView = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.foodstuff_layout, parent, false);
            return new MyViewHolder(foodstuffView);
        } else {
            LinearLayout nutritionProgress = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nutrition_progress_layout, parent, false);
            return new MyViewHolder(nutritionProgress);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        int itemType = getItemViewType(position);
        if (itemType == ITEM_TYPE_FOODSTUFF) {
            ViewGroup item = ((MyViewHolder)holder).getItem();
            final Foodstuff foodstuff = ((FoodstuffData) getItem(position)).getHistoryEntry().getFoodstuff();
            double weight = foodstuff.getWeight();
            setTextViewText(item, R.id.name, context.getString(
                    R.string.foodstuff_name_and_weight, foodstuff.getName(), foodstuff.getWeight()));
            setTextViewText(item, R.id.protein, context.getString(
                    R.string.one_digit_precision_float, foodstuff.getProtein() * weight * 0.01));
            setTextViewText(item, R.id.fats, context.getString(
                    R.string.one_digit_precision_float, foodstuff.getFats() * weight * 0.01));
            setTextViewText(item, R.id.carbs, context.getString(
                    R.string.one_digit_precision_float, foodstuff.getCarbs() * weight * 0.01));
            setTextViewText(item, R.id.calories, context.getString(
                    R.string.one_digit_precision_float, foodstuff.getCalories() * weight * 0.01));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    observer.onItemClicked(foodstuff, holder.getAdapterPosition());
                }
            });
        } else if (itemType == ITEM_TYPE_PROGRESS) {
            ViewGroup item = ((MyViewHolder)holder).getItem();
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
            ViewGroup nutritionLayout = ((MyViewHolder) holder).getItem();
            setProgressBarValue(nutritionLayout.findViewById(R.id.protein_progress),
                    ateProtein, rates.getProtein());
            setProgressBarValue(nutritionLayout.findViewById(R.id.fat_progress),
                    ateFats, rates.getFats());
            setProgressBarValue(nutritionLayout.findViewById(R.id.carbs_progress),
                    ateCarbs, rates.getCarbs());
            setProgressBarValue(nutritionLayout.findViewById(R.id.calories_progress),
                    ateCalories, rates.getCalories());
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
        addItemImpl(historyEntry);
        // Уведомляем recycler view об изменении всего множества, а не о вставки 1 элемента, т.к.
        // addItemImpl() может добавить как 1 элемент (если вставляем фудстаф в уже существующую дату),
        // так и 2 элемента (если вставляем фудстаф в новую дату, то вставляется и фудстаф и дата) -
        // придётся из addItemImpl() возвращать List<InsertedIndex>,
        // и по нему итерироваться, уведомляя recycler view.
        notifyDataSetChanged();
    }

    public void addItems(List<HistoryEntry> historyEntries) {
        for (HistoryEntry entry : historyEntries) {
            addItemImpl(entry);
        }
        notifyDataSetChanged();
    }

    /**
     * Impl-метод не уведомляет recycler view об изменениях.
     * Это нужно специально чтобы иметь возможность вставлять большие батчи фудстафов
     * в адаптер с уведомлением recycler view только в конце вставки батча (см. addItems)
     * - если уведомлять recycler view после вставки каждого элемента, то такая вставка начинает
     * очень заметно тормозить, т.к. recycler view зачем-то производит тяжёлые операции
     * при каждом уведомлении об изменениях, даже если изменяемые элементы точно не видны.
     */
    private void addItemImpl(HistoryEntry historyEntry) {
        int dateIndex = findHistoryEntryDateIndex(historyEntry);
        // если нужная дата найдена
        if (dateIndex != -1) {
            addFoodstuffToDate(dateIndex, historyEntry);
            return;
        }

        // если ещё ни одной даты нет - добавить новую + продукт
        if (data.isEmpty()) {
            addDate(historyEntry.getTime(), 0);
            addFoodstuffToDate(0, historyEntry);
        } else {
            int earlierDateIndex = findEarlierDateIndex(historyEntry.getTime());
            if (earlierDateIndex != -1) {
                // замещаем старую дату новой
                addDate(historyEntry.getTime(), earlierDateIndex);
                addFoodstuffToDate(earlierDateIndex, historyEntry);
                return;
            }
            // если более ранней даты нет
            int endIndex = data.size();
            addDate(historyEntry.getTime(), endIndex);
            addFoodstuffToDate(endIndex, historyEntry);
        }
    }

    private int findHistoryEntryDateIndex(HistoryEntry historyEntry) {
        long newDaySinceBC = calculateDaySinceBC(historyEntry.getTime());
        for (int index = 0; index < data.size(); index++) {
            if (data.get(index) instanceof DateData) {
                DateData currentDate = (DateData) data.get(index);
                if (newDaySinceBC == currentDate.getDaySinceBC()) {
                    return index;
                }
            }
        }
        return -1;
    }

    private void addFoodstuffToDate(int dateIndex, HistoryEntry historyEntry) {
        int index = findWhereToAddHistoryEntry(dateIndex, historyEntry);
        data.add(index, new FoodstuffData(historyEntry));
    }

    private void addDate(Date date, int dateIndex) {
        data.add(dateIndex, new DateData(date));
    }

    private int findEarlierDateIndex(Date currentDate) {
        for (int index = 0; index < data.size(); index++) {
            // если дата по индексу - более ранняя
            if (data.get(index) instanceof DateData) {
                DateData dateData = (DateData) data.get(index);
                if (dateData.getDate().compareTo(currentDate) < 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private int findWhereToAddHistoryEntry(int dateIndex, HistoryEntry historyEntry) {
        for (int index = dateIndex + 1; index < data.size(); index++) {
            if (data.get(index) instanceof DateData) {
                return index;
            }
            FoodstuffData foodstuffData = (FoodstuffData) data.get(index);
            Date date = foodstuffData.getHistoryEntry().getTime();
            Date currentDate = historyEntry.getTime();
            if (date.compareTo(currentDate) < 0) {
                return index;
            }
        }
        return data.size();
    }

    public void deleteItem(int displayedPosition) {
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
            notifyItemChanged(requiredDateIndex);
        }
    }

    public void replaceItem(HistoryEntry newHistoryEntry, int displayedPosition) {
        data.set(displayedPosition, new FoodstuffData(newHistoryEntry));
        notifyItemChanged(displayedPosition);

        // пересчитываем прогрессбары
        for (int index = displayedPosition; index >= 0; --index) {
            if (data.get(index) instanceof DateData) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }

    private void setProgressBarValue(View layout, double currentValue, double maxValue) {
        ProgressBar progressBar = layout.findViewById(R.id.progress_bar);
        progressBar.setMax((int) Math.round(maxValue));
        progressBar.setProgress((int) Math.round(currentValue));

        Formatter formatter = new Formatter();
        formatter.format("%.1f/%.1f", currentValue, maxValue);
        TextView textView = layout.findViewById(R.id.progress_text_view);
        textView.setText(formatter.toString());
    }
}
