package com.example.trackfield.adapters.recycleradapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackfield.R;
import com.example.trackfield.items.headers.RecyclerItem;

import java.util.ArrayList;

@Deprecated public class IntervalRecyclerAdapter extends RecyclerAdapter {

    public IntervalRecyclerAdapter(ArrayList<RecyclerItem> itemList, int originId, Context c) {
        super(itemList, c);
        this.originId = originId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == ITEM_ITEM) {
            ConstraintLayout cl = (ConstraintLayout) inflater.inflate(R.layout.item_exercise_distance, parent, false);
            return new IntervalExerciseVH(cl);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

}
