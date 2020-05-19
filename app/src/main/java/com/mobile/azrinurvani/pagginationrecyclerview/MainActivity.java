package com.mobile.azrinurvani.pagginationrecyclerview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mobile.azrinurvani.pagginationrecyclerview.adapter.PagginationAdapter;
import com.mobile.azrinurvani.pagginationrecyclerview.api.MovieApi;
import com.mobile.azrinurvani.pagginationrecyclerview.api.MovieService;
import com.mobile.azrinurvani.pagginationrecyclerview.models.Movie;
import com.mobile.azrinurvani.pagginationrecyclerview.models.TopRatedMovies;
import com.mobile.azrinurvani.pagginationrecyclerview.utils.PagginationScrollListener;
import com.mobile.azrinurvani.pagginationrecyclerview.utils.PaginationAdapterCallback;

import java.util.List;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//todo 9
public class MainActivity extends AppCompatActivity implements PaginationAdapterCallback {

    private static final String TAG = "MainActivity";

    PagginationAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    RecyclerView rv;
    ProgressBar progressBar;
    LinearLayout errorLayout;
    Button btnRetry;
    TextView txtError;
    SwipeRefreshLayout swipeRefreshLayout;

    private static final int PAGE_START = 1;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    // limiting to 5 for this tutorial, since total pages in actual API is very large. Feel free to modify.
    private static final int TOTAL_PAGES = 5;
    private int currentPage = PAGE_START;

    private MovieService movieService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initRecyclerView();

        initDataForRecyclerView();
    }

    private void initView() {
        rv = findViewById(R.id.main_recycler);
        progressBar = findViewById(R.id.main_progress);
        errorLayout = findViewById(R.id.error_layout);
        btnRetry = findViewById(R.id.error_btn_retry);
        txtError = findViewById(R.id.error_txt_cause);
        swipeRefreshLayout = findViewById(R.id.main_swiperefresh);
    }

    private void initRecyclerView() {
        adapter = new PagginationAdapter(this);

        linearLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        rv.setLayoutManager(linearLayoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new PagginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage +=1;
                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    private void initDataForRecyclerView() {
        movieService = MovieApi.getClient(this).create(MovieService.class);

        loadFirstPage();
        btnRetry.setOnClickListener(view->loadFirstPage());
        swipeRefreshLayout.setOnRefreshListener(this::doRefresh);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                // Signal SwipeRefreshLayout to start the progress indicator
                swipeRefreshLayout.setRefreshing(true);
                doRefresh();
        }

        return super.onOptionsItemSelected(item);

    }

    private void loadFirstPage() {
        Log.d(TAG, "loadFirstPage: ");

        // To ensure list is visible when retry button in error view is clicked
        hideErrorView();
        currentPage = PAGE_START;

        callTopRatedMoviesApi().enqueue(new Callback<TopRatedMovies>() {
            @Override
            public void onResponse(Call<TopRatedMovies> call, Response<TopRatedMovies> response) {
                hideErrorView();
                List<Movie> results = fetchResults(response);
                progressBar.setVisibility(View.GONE);
                adapter.addAll(results);

                if (currentPage<=TOTAL_PAGES){
                    adapter.addLoadingFooter();
                }else{
                    isLastPage = true;
                }
            }

            @Override
            public void onFailure(Call<TopRatedMovies> call, Throwable t) {
                Log.e(TAG,"loadFirstPage.onFailure : "+t.getLocalizedMessage());
                showErrorView(t);
            }
        });


    }

    private List<Movie> fetchResults(Response<TopRatedMovies> response) {
        TopRatedMovies topRatedMovies = response.body();
        return topRatedMovies.getMovies();
    }


    private void loadNextPage() {
        Log.d(TAG, "loadNextPage: " + currentPage);

        callTopRatedMoviesApi().enqueue(new Callback<TopRatedMovies>() {
            @Override
            public void onResponse(Call<TopRatedMovies> call, Response<TopRatedMovies> response) {
                adapter.removeLoadingFooter();
                isLoading = false;
                List<Movie> results = fetchResults(response);
                adapter.addAll(results);

                if (currentPage != TOTAL_PAGES){
                    adapter.addLoadingFooter();
                }else{
                    isLastPage = true;
                }
            }

            @Override
            public void onFailure(Call<TopRatedMovies> call, Throwable t) {
                Log.e(TAG,"loadNextPage.onFailure : "+t.getLocalizedMessage());
                adapter.showRetry(true,fetchErrorMessage(t));
            }
        });

    }

    private void doRefresh() {
        progressBar.setVisibility(View.VISIBLE);
        if (callTopRatedMoviesApi().isExecuted()){
            callTopRatedMoviesApi().cancel();
        }

        // TODO: Check if data is stale.
        //  Execute network request if cache is expired; otherwise do not update data.
        adapter.getMovies().clear();
        adapter.notifyDataSetChanged();
        loadFirstPage();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showErrorView(Throwable t) {
        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            txtError.setText(fetchErrorMessage(t));
        }
    }

    private void hideErrorView() {
        if (errorLayout.getVisibility()==View.VISIBLE){
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private String fetchErrorMessage(Throwable t) {
        String errMsg = getResources().getString(R.string.error_msg_unknown);
        if (!isNetworkConnected()){
            errMsg = getResources().getString(R.string.error_msg_no_internet);
        }else if (t instanceof TimeoutException){
            errMsg = getResources().getString(R.string.error_msg_timeout);
        }

        return errMsg;
    }



    private Call<TopRatedMovies> callTopRatedMoviesApi(){
        return movieService.getTopRatedMovies(
                getString(R.string.my_api_key),
        "en_US",
                currentPage
        );
    }
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public void retryPageLoad() {
        loadNextPage();
    }
}
