package com.example.trackfield.adapters.recycler_adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackfield.R;
import com.example.trackfield.items.headers.RecyclerItem;
import com.example.trackfield.items.headers.archive.GraphOld;

import java.util.ArrayList;

public class DiExRecyclerAdapter extends RecyclerAdapter {

    int distance;

    public DiExRecyclerAdapter(ArrayList<RecyclerItem> itemList, int distance, int originId, Context c) {
        super(itemList, c);
        this.distance = distance;
        this.originId = originId;
    }
    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == ITEM_ITEM) {
            ConstraintLayout cl = (ConstraintLayout) inflater.inflate(R.layout.layout_item_exercise_distance, parent, false);
            return new DistanceExerciseVH(cl);
        }
        else if (viewType == ITEM_GRAPH_OLD) {
            ConstraintLayout cl = GraphOld.inflateLayout(inflater, parent);
            return new GraphVH(parent, cl);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

}
