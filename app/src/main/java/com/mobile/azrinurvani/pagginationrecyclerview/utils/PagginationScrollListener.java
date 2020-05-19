package com.mobile.azrinurvani.pagginationrecyclerview.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//todo 2
public abstract class PagginationScrollListener extends RecyclerView.OnScrollListener {

    LinearLayoutManager layoutManager;

    public PagginationScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        if (!isLoading() && !isLastPage()){
            if ((visibleItemCount+firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition>=0  ){
                loadMoreItems();
            }
        }

    }
    protected abstract void loadMoreItems();
    public abstract int getTotalPageCount();
    public abstract boolean isLastPage();
    public abstract boolean isLoading();
}
