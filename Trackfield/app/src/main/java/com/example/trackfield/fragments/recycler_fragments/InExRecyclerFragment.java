package com.example.trackfield.fragments.recycler_fragments;

import android.os.Bundle;
import android.view.View;

import com.example.trackfield.activities.ViewActivity;
import com.example.trackfield.adapters.recycler_adapters.InExRecyclerAdapter;
import com.example.trackfield.adapters.recycler_adapters.RecyclerAdapter;
import com.example.trackfield.items.Exerlite;
import com.example.trackfield.items.headers.RecyclerItem;
import com.example.trackfield.items.headers.Sorter;
import com.example.trackfield.toolbox.C;
import com.example.trackfield.toolbox.Prefs;

import java.util.ArrayList;

public class InExRecyclerFragment extends RecyclerFragment {

    private final String[] sortModesTitle = { "Date", "Distance", "Time", "Pace" };
    private final C.SortMode[] sortModes = { C.SortMode.DATE, C.SortMode.DISTANCE, C.SortMode.TIME, C.SortMode.PACE };
    private final boolean[] smallestFirsts = { false, false, true, true };

    private String interval;
    private int originId;

    private final static String BUNDLE_INTERVAL = "interval";
    private final static String BUNDLE_ORIGINID = "originId";

    ////

    public static InExRecyclerFragment newInstance(String interval, int originId) {
        InExRecyclerFragment instance = new InExRecyclerFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_INTERVAL, interval);
        bundle.putInt(BUNDLE_ORIGINID, originId);
        instance.setArguments(bundle);
        return instance;
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            interval = bundle.getString(BUNDLE_INTERVAL, "");
            originId = bundle.getInt(BUNDLE_ORIGINID, -1);
        }
    }

    @Override protected ArrayList<RecyclerItem> getRecyclerItems() {

        ArrayList<Exerlite> exerliteList = reader.getExerlitesByInterval(interval, sortMode, smallestFirst);
        //ArrayList<Exerlite> chronoList = reader.getExerlitesByInterval(interval, C.SortMode.DATE, false);
        ArrayList<RecyclerItem> itemList = new ArrayList<>();

        /*Graph graph = new Graph(chronoList, Graph.DataX.INDEX, Graph.DataY.PACE);
        itemList.add(graph);*/
        Sorter sorter = newSorter(sortModes, sortModesTitle);
        itemList.add(sorter);

        addHeadersAndItems(itemList, exerliteList);
        /*if (sortMode == C.SortMode.DATE) {
            int year = -1; int newYear;
            for (Exerlite e : exerliteList) {
                if ((newYear = e.getDate().getYear()) != year) {
                    itemList.add(new Header(newYear + "", Header.Type.REC));
                    year = newYear;
                }
                itemList.add(e);
            }
        }
        else if ((sortMode == C.SortMode.PACE || sortMode == C.SortMode.TIME) && smallestFirst && exerliteList.size() > 10) {
            for (int i = 0; i < exerliteList.size(); i++) {
                if (i % 10 == 0) {
                    itemList.add(new Header("Top " + (i+10), Header.Type.REC));
                }
                itemList.add(exerliteList.get(i));
            }
        }
        else { itemList.addAll(exerliteList); }*/

        return itemList;
    }
    @Override protected void setSortModes() {
        sortMode = Prefs.getSortModePref(C.Layout.EXERCISE_ROUTE);
        smallestFirst = Prefs.getSmallestFirstPref(C.Layout.EXERCISE_ROUTE);
    }
    @Override protected void getAdapter() {
        adapter = new InExRecyclerAdapter(items, originId, a);
    }
    @Override protected void getPrefs() {
        sortMode = Prefs.getSortModePref(C.Layout.EXERCISE_ROUTE);
        smallestFirst = Prefs.getSmallestFirstPref(C.Layout.EXERCISE_ROUTE);
    }
    @Override protected void setPrefs() {
        Prefs.setSortModePref(C.Layout.EXERCISE_ROUTE, sortMode);
        Prefs.setSmallestFirstPref(C.Layout.EXERCISE_ROUTE, smallestFirst);
    }

    @Override public void onItemClick(View view, int position, int itemType) {
        if (itemType == RecyclerAdapter.ITEM_ITEM) {
            int _id = ((Exerlite) items.get(position)).get_id();
            if (originId != _id) ViewActivity.startActivity(a, _id, ViewActivity.FROM_INTERVAL);
        }
        super.onItemClick(itemType, sortModes, sortMode, sortModesTitle, smallestFirsts, smallestFirst);
    }

}
