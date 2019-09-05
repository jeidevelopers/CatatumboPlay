package com.ufpso.catatumboplay.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.ufpso.catatumboplay.R;
import com.ufpso.catatumboplay.listener.OnLoadMoreVideosListener;
import com.ufpso.catatumboplay.model.Category;
import com.ufpso.catatumboplay.network.APIClient;
import com.ufpso.catatumboplay.network.APIConstants;
import com.ufpso.catatumboplay.network.APIInterface;
import com.ufpso.catatumboplay.ui.activity.MainActivity;
import com.ufpso.catatumboplay.ui.adapter.CategoriesAdapter;
import com.ufpso.catatumboplay.util.NetworkUtils;
import com.ufpso.catatumboplay.util.UiUtils;
import com.ufpso.catatumboplay.util.sharedpref.PrefKeys;
import com.ufpso.catatumboplay.util.sharedpref.PrefUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.ufpso.catatumboplay.network.APIConstants.*;

public class CategoryFragment extends Fragment implements OnLoadMoreVideosListener {

    public static String categoryBeingViewed = "";
    MainActivity activity;
    Unbinder unbinder;
    @BindView(R.id.categoriesRecycler)
    RecyclerView categoriesRecycler;
    APIInterface apiInterface;
    @BindView(R.id.noResultLayout)
    TextView noResultLayout;
    @BindView(R.id.shimmer)
    ShimmerFrameLayout shimmer;
    private CategoriesAdapter categoryAdapter;
    private ArrayList<Category> categories;
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LinearLayoutManager llmanager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (llmanager.findLastCompletelyVisibleItemPosition() == (categoryAdapter.getItemCount() - 1)) {
                categoryAdapter.showLoading();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();
        categories = new ArrayList<>();
        apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_category, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setUpCategories();
        super.onViewCreated(view, savedInstanceState);
    }

    private void setUpCategories() {
        categoryAdapter = new CategoriesAdapter(activity, this, categories);
        categoriesRecycler.setLayoutManager(new GridLayoutManager(activity, 2));
        categoriesRecycler.setHasFixedSize(true);
        categoriesRecycler.setAdapter(categoryAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getCategoryList(0);
    }

    protected void getCategoryList(int skip) {
        if (skip == 0) {
            shimmer.setVisibility(View.VISIBLE);
            noResultLayout.setVisibility(View.GONE);
            categoriesRecycler.setVisibility(View.GONE);
            categoriesRecycler.setOnScrollListener(scrollListener);
        }
        PrefUtils prefUtils = PrefUtils.getInstance(activity);
        Call<String> call = apiInterface.getCategories(prefUtils.getIntValue(PrefKeys.USER_ID, -1)
                , prefUtils.getStringValue(PrefKeys.SESSION_TOKEN, "")
                , prefUtils.getIntValue(PrefKeys.ACTIVE_SUB_PROFILE, -1)
                , skip);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (isAdded()) {
                    if (skip == 0) {
                        categories.clear();
                        shimmer.setVisibility(View.GONE);
                    }

                    JSONObject categoryListObject = null;
                    try {
                        categoryListObject = new JSONObject(response.body());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (categoryListObject != null) {
                        if (categoryListObject.optString(Params.SUCCESS).equals(Constants.TRUE)) {
                            JSONArray categoryArray = categoryListObject.optJSONArray(Params.DATA);
                            if(categoryArray!=null){
                                for (int i = 0; i < categoryArray.length(); i++) {
                                    JSONObject object = categoryArray.optJSONObject(i);
                                    Category category = new Category();
                                    category.setId(object.optInt(Params.CATEGORY_ID));
                                    category.setTitle(object.optString(Params.NAME));
                                    category.setThumbnailUrl(object.optString(Params.PICTURE));
                                    categories.add(category);
                                }
                                if (categoryArray.length() == 0) {
                                    categoriesRecycler.removeOnScrollListener(scrollListener);
                                }
                                onDataChanged();
                            }
                        } else {
                            UiUtils.showShortToast(activity, categoryListObject.optString(Params.ERROR_MESSAGE));
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (isAdded()) {
                    shimmer.setVisibility(View.GONE);
                    noResultLayout.setVisibility(View.VISIBLE);
                    NetworkUtils.onApiError(activity);
                }
            }
        });
    }

    private void onDataChanged() {
        if (isAdded()) {
            categoryAdapter.notifyDataSetChanged();
            noResultLayout.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
            categoriesRecycler.setVisibility(categories.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onLoadMore(int skip) {
        getCategoryList(skip);
    }
}
