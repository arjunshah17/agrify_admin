package com.example.agrify_admin.fragments;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrify_admin.ProductActivity.ProductActivity;
import com.example.agrify_admin.R;
import com.example.agrify_admin.StoreDetailActivity;
import com.example.agrify_admin.adapter.StoreAdapter;
import com.example.agrify_admin.auth.LoginActivity;
import com.example.agrify_admin.databinding.FragmentStoreBinding;
import com.example.agrify_admin.listener.NavigationIconClickListener;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import es.dmoral.toasty.Toasty;
import spencerstudios.com.bungeelib.Bungee;

/**
 * A simple {@link Fragment} subclass.
 */
public class StoreFragment extends Fragment implements StoreAdapter.OnStoreSelectedListener {
    private static final String TAG = "MainActivity";
    private static final int LIMIT = 50;
    private static String[] CATEGORES_NAMES;
    FragmentStoreBinding bind;
    private String selectedCategory = "all";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore mFirestore;
    private Query mQuery;
    private NavigationIconClickListener navigationIconClickListener;
    private StoreAdapter mAdapter;
    public StoreFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        CATEGORES_NAMES = getResources().getStringArray(R.array.categories_names);//add category from array


        bind = DataBindingUtil.inflate(inflater, R.layout.fragment_store, container, false);//inflaste layout


        firebaseAuth = FirebaseAuth.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            bind.productGrid.setBackground(getContext().getDrawable(R.drawable.store_item_background));//set curve background
        }


        setUpToolbar();

        initFirestore();
        initRecyclerView();
        initListView();

        bind.signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
                navigationIconClickListener.closeMenu();
            }
        });
        bind.storeRecycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {

                    bind.fabButton.collapse(true);
                } else {
                    bind.fabButton.expand(true);
                    bind.appBar.setVisibility(View.VISIBLE);

                }
            }
        });

        bind.fabButton.toggle(true);
        bind.fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ProductActivity.class));
            }
        });

        return bind.getRoot();
    }

    private void initListView() {

        bind.categoryListView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.category_list_item, CATEGORES_NAMES));
        bind.categoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = ((TextView) view).getText().toString();
                Toasty.info(getActivity(), text, Toast.LENGTH_LONG).show();
                navigationIconClickListener.closeMenu();
                loadStoreProducts(text);

            }
        });
    }

    void loadStoreProducts(String text) {

        productLoadingState(true);
        if (text.equals("all")) {
            mQuery = mFirestore.collection("store").orderBy("name");
            selectedCategory = text;
        } else {
            selectedCategory = text;
            mQuery = mFirestore.collection("store").orderBy("name").whereEqualTo("category", text);
        }


        mAdapter.setQuery(mQuery);
        mAdapter.notifyDataSetChanged();

    }





    private void initFirestore() {
        // TODO(developer): Implement
        mFirestore = FirebaseFirestore.getInstance();
        mQuery = mFirestore.collection("store").orderBy("name");

    }

    private void initRecyclerView() {

        if (mQuery == null) {
            Log.w(TAG, "No query, not initializing RecyclerView");
        }

        mAdapter = new StoreAdapter(mQuery, this, getActivity()) {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                super.onEvent(documentSnapshots, e);
                productLoadingState(false);

                if(getItemCount()==0)
                {
                    noProductFound(true);
                }
                else {
                    noProductFound(false);
                }
            }

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
if(getItemCount()==0)
{
    noProductFound(true);

}
else {
    noProductFound(false);
}

            }


        };

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        bind.storeRecycleView.setHasFixedSize(true);
        bind.storeRecycleView.setLayoutManager(gridLayoutManager);


        bind.storeRecycleView.setAdapter(mAdapter);


    }

    @Override
    public void onStoreSelected(DocumentSnapshot store,View SharedView) {
        Intent intent = new Intent(getActivity(), StoreDetailActivity.class);
        intent.putExtra(StoreDetailActivity.KEY_STORE_ID, store.getId());
        String transitionName = getString(R.string.store_product_transition);

        ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), SharedView, transitionName);
        startActivity(intent,transitionActivityOptions.toBundle());
        Bungee.inAndOut(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start sign in if necessary

        // Start listening for Firestore updates
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }


    private void setUpToolbar() {

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(bind.appBar);
        }
        navigationIconClickListener = new NavigationIconClickListener(getContext(), bind.productGrid, new AccelerateDecelerateInterpolator(), getContext().getDrawable(R.drawable.shr_menu), getContext().getDrawable(R.drawable.shr_close_menu));

        bind.appBar.setNavigationOnClickListener(navigationIconClickListener);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.shr_toolbar_menu, menu);
        super.onCreateOptionsMenu(menu, menuInflater);

        final MenuItem search_item = menu.findItem(R.id.store_searchBar);
        SearchView searchView = (SearchView) search_item.getActionView();
        searchView.setFocusable(false);
        searchView.setQueryHint("Search");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {


                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                Query queryName;
                if(query!=null) {
                    queryName = mFirestore.collection("store").orderBy("name").startAt(query.toLowerCase()).endAt(query.toLowerCase()+ "\uf8ff");
                    mAdapter.setQuery(queryName);

                }


                return false;
            }
        });
      searchView.setOnCloseListener(new SearchView.OnCloseListener() {
          @Override
          public boolean onClose() {
              mAdapter.setQuery(mQuery);
              return false;
          }
      });
    }

    void signOut() {
        if (firebaseAuth.getCurrentUser() != null) {
            AuthUI.getInstance().signOut(getActivity()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toasty.info(getActivity(), "log out successfully", Toasty.LENGTH_SHORT).show();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        Bungee.windmill(getActivity());
                    } else {
                        Toasty.error(getActivity(), task.getException().getLocalizedMessage(), Toasty.LENGTH_SHORT).show();

                    }
                }
            });

        }

    }

    void productLoadingState(boolean state)
    {
        if(state)
        {bind.storeRecycleView.setVisibility(View.GONE);
            bind.shimmerRecyclerView.showShimmerAdapter();



        }
        else
        {bind.storeRecycleView.setVisibility(View.VISIBLE);
            bind.shimmerRecyclerView.hideShimmerAdapter();
            // TODO stop shrimmer effect

        }
    }
    void noProductFound(boolean state)
    {
        if(state)
        {
            bind.storeRecycleView.setVisibility(View.GONE);


            bind.animationView.playAnimation();
            bind.animationLayout.setVisibility(View.VISIBLE);
        }
        else {
bind.animationLayout.setVisibility(View.GONE);
            bind.storeRecycleView.setVisibility(View.VISIBLE);
            bind.animationView.cancelAnimation();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 1:
               mAdapter.edit(item.getGroupId());
                return true;
            case 2:

             mAdapter.delete(item.getGroupId());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}