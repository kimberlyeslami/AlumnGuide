package com.example.alumnguide.fragments;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alumnguide.AddJobPostActivity;
import com.example.alumnguide.Login;
import com.example.alumnguide.MainActivity;
import com.example.alumnguide.R;
import com.example.alumnguide.adapters.AdapterJobPosts;
import com.example.alumnguide.models.ModelJobPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JobsFragment extends Fragment {
    FirebaseAuth firebaseAuth;

    RecyclerView recyclerView;
    List<ModelJobPost> jobPostList;

    AdapterJobPosts adapterJobPosts;
    ActionBar actionBar;
    String myUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_jobs,container,false);

        firebaseAuth = FirebaseAuth.getInstance();

        //recycler view and its properties
        recyclerView = view.findViewById(R.id.jobPostsRecyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first, for this load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        //set layout to recycler view
        recyclerView.setLayoutManager(layoutManager);

        //innit post list
        jobPostList = new ArrayList<>();
        loadJobPosts();

        return view;
    }

    private void loadJobPosts() {
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("JobPosts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                jobPostList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelJobPost modelJobPost = ds.getValue(ModelJobPost.class);

                    jobPostList.add(modelJobPost);

                    //adapter
                    adapterJobPosts = new AdapterJobPosts(getActivity(), jobPostList);
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterJobPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // in case of error
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPosts(final String searchQuery) {
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("JobPosts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                jobPostList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelJobPost modelJobPost = ds.getValue(ModelJobPost.class);

                    if (modelJobPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelJobPost.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())) {
                        jobPostList.add(modelJobPost);
                    }
                    //adapter
                    adapterJobPosts = new AdapterJobPosts(getActivity(), jobPostList);
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterJobPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // in case of error
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchPosts(query);
                } else {
                    loadJobPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s)) {
                    searchPosts(s);
                } else {
                    loadJobPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }
    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
        }else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            startActivity(new Intent(getActivity(), Login.class));
            checkUserStatus();

        }
        if (id == R.id.action_add_post) {
            startActivity(new Intent(getActivity(), AddJobPostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}



