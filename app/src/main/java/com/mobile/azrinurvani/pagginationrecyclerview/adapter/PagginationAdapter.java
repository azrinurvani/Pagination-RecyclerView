package com.mobile.azrinurvani.pagginationrecyclerview.adapter;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.mobile.azrinurvani.pagginationrecyclerview.R;
import com.mobile.azrinurvani.pagginationrecyclerview.models.Movie;
import com.mobile.azrinurvani.pagginationrecyclerview.utils.GlideApp;
import com.mobile.azrinurvani.pagginationrecyclerview.utils.GlideRequest;
import com.mobile.azrinurvani.pagginationrecyclerview.utils.PaginationAdapterCallback;

import java.util.ArrayList;
import java.util.List;

//todo 7
public class PagginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // flag for footer ProgressBar (i.e. last item of list)
    private boolean isLoadingAdded = false;
    private boolean retryPageLoad = false;
    private String errorMsg;

    private static final String BASE_URL_IMG = "https://image.tmdb.org/t/p/w200";
    private PaginationAdapterCallback mCallback;


    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private static final int HERO = 2;

    private List<Movie> movies;
    private Context context;

    public PagginationAdapter(Context context) {
        this.context = context;
        this.movies = new ArrayList<>();
    }

    public List<Movie> getMovies(){
        return movies;
    }
    public void setMovies(List<Movie> movies){
        this.movies = movies;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType){
            case HERO : {
                View viewHero = inflater.inflate(R.layout.item_hero,parent,false);
                viewHolder = new HeroVH(viewHero);
                break;
            }

            case ITEM :{
                View viewItem = inflater.inflate(R.layout.item_list,parent, false);
                viewHolder = new MovieVH(viewItem);
                break;
            }

            case LOADING : {
                View viewLoading = inflater.inflate(R.layout.item_progress,parent,false);
                viewHolder = new LoadingVH(viewLoading);
                break;
            }

        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Movie movie = movies.get(position);
        switch (getItemViewType(position)){
            case HERO :{
                final HeroVH heroVH = (HeroVH) holder;
                heroVH.mMovieTitle.setText(movie.getTitle());
                heroVH.mMovieDesc.setText(movie.getOverview());
                heroVH.mYear.setText(formatYearLabel(movie));

                loadImage(movie.getBackdropPath()).into(heroVH.mPosterImg);

                break;

            }
            case ITEM:{
                final MovieVH movieVH = (MovieVH) holder;
                movieVH.mMovieTitle.setText(movie.getTitle());
                movieVH.mYear.setText(formatYearLabel(movie));
                movieVH.mMovieDesc.setText(movie.getOverview());

                // load movie thumbnail
                loadImage(movie.getPosterPath())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                movieVH.mProgress.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // image ready, hide progress now
                                movieVH.mProgress.setVisibility(View.GONE);
                                return false;  // return false if you want Glide to handle everything else.
                            }
                        })
                        .into(movieVH.mPosterImg);

                break;
            }
            case LOADING : {
                final LoadingVH loadingVH = (LoadingVH) holder;
                if (retryPageLoad){
                    loadingVH.mErrorLayout.setVisibility(View.VISIBLE);
                    loadingVH.mProgressBar.setVisibility(View.GONE);

                    loadingVH.mErrorTxt.setText(
                            errorMsg!=null ? errorMsg :context.getString(R.string.error_msg_unknown));
                }else{
                    loadingVH.mErrorTxt.setVisibility(View.GONE);
                    loadingVH.mProgressBar.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return movies == null ? 0 : movies.size();
    }

    @Override
    public int getItemViewType(int position) {

        if (position==0){
            return HERO;
        }else {
            return (position==movies.size() - 1 &&isLoadingAdded)? LOADING: ITEM;
        }
    }

     /*
        Helpers - bind Views
   _________________________________________________________________________________________________
    */

    /**
     * @param result
     * @return [releasedate] | [2letterlangcode]
     */
    private String formatYearLabel(Movie result) {
        return result.getReleaseDate().substring(0, 4)  // we want the year only
                + " | "
                + result.getOriginalLanguage().toUpperCase();
    }

    private GlideRequest<Drawable> loadImage(@NonNull String posterPath) {
        return GlideApp
                .with(context)
                .load(BASE_URL_IMG + posterPath)
                .centerCrop();
    }



    /*
      Helpers - Pagination
   _________________________________________________________________________________________________
    */

    public void add(Movie m){
        movies.add(m);
        notifyItemInserted(movies.size()-1);
    }

    public void addAll(List<Movie> movieResults){
        for (Movie movie : movieResults){
            add(movie);
        }
    }

    public void remove(Movie m){
        int position = movies.indexOf(m);
        if (position>-1){
            movies.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear(){
        isLoadingAdded = false;
        while (getItemCount()>0){
            remove(getItem(0));
        }
    }

    public boolean isEmpty(){
        return getItemCount() == 0;
    }

    public void addLoadingFooter(){
        isLoadingAdded = true;
        add(new Movie());
    }

    public void removeLoadingFooter(){
        isLoadingAdded = false;
        int position = movies.size()-1;
        Movie movie = getItem(position);

        if (movie!=null){
            movies.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Movie getItem(int position) {
        return movies.get(position);
    }
    /*
    * End of helper paggination
    */


    /**
     * Displays Pagination retry footer view along with appropriate errorMsg
     *
     * @param show
     * @param errorMsg to display if page load fails
     */
    public void showRetry(boolean show, @Nullable String errorMsg){
        retryPageLoad = show;
        notifyItemChanged(movies.size()-1);

        if (errorMsg!=null){
            this.errorMsg = errorMsg;
        }
    }
    /*
    * End of paggination retry footer
    */


    /*
    View Holders
    _________________________________________________________________________________________________
    */

    /**
     * Header ViewHolder
     */
    protected class HeroVH extends RecyclerView.ViewHolder {
        private TextView mMovieTitle;
        private TextView mMovieDesc;
        private TextView mYear; // displays "year | language"
        private ImageView mPosterImg;

        public HeroVH(View itemView) {
            super(itemView);

            mMovieTitle = itemView.findViewById(R.id.movie_title);
            mMovieDesc = itemView.findViewById(R.id.movie_desc);
            mYear = itemView.findViewById(R.id.movie_year);
            mPosterImg = itemView.findViewById(R.id.movie_poster);
        }
    }

    /**
     * Main list's content ViewHolder
     */
    protected class MovieVH extends RecyclerView.ViewHolder {
        private TextView mMovieTitle;
        private TextView mMovieDesc;
        private TextView mYear; // displays "year | language"
        private ImageView mPosterImg;
        private ProgressBar mProgress;

        public MovieVH(View itemView) {
            super(itemView);

            mMovieTitle = itemView.findViewById(R.id.movie_title);
            mMovieDesc = itemView.findViewById(R.id.movie_desc);
            mYear = itemView.findViewById(R.id.movie_year);
            mPosterImg = itemView.findViewById(R.id.movie_poster);
            mProgress = itemView.findViewById(R.id.movie_progress);
        }
    }

    protected class LoadingVH extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ProgressBar mProgressBar;
        private ImageButton mRetryBtn;
        private TextView mErrorTxt;
        private LinearLayout mErrorLayout;

        public LoadingVH(@NonNull View itemView) {
            super(itemView);

            mProgressBar = itemView.findViewById(R.id.loadmore_progress);
            mRetryBtn = itemView.findViewById(R.id.loadmore_retry);
            mErrorTxt = itemView.findViewById(R.id.loadmore_errortxt);
            mErrorLayout = itemView.findViewById(R.id.loadmore_errorlayout);

            mRetryBtn.setOnClickListener(this);
            mErrorLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.loadmore_retry:
                case R.id.loadmore_errorlayout:

                    showRetry(false, null);
                    mCallback.retryPageLoad();

                    break;
            }

        }
    }
}
