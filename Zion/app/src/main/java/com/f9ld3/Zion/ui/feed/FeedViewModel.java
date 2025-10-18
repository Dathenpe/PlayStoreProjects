package com.f9ld3.Zion.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends ViewModel {

    private static final String TAG = "FeedViewModel";
    private final MutableLiveData<List<Post>> mPosts = new MutableLiveData<>();
    public LiveData<List<Post>> getPosts() { return mPosts; }

    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() { return mIsLoading; }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration firestoreListener;

    private boolean initialLoad = true; // Flag for initial skeleton display

    public FeedViewModel() {
        fetchPosts();
    }

    private void fetchPosts() {
        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        mIsLoading.setValue(true); // Start loading
        Log.d(TAG, "Fetching posts...");

        firestoreListener = query.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                mPosts.setValue(null);
                mIsLoading.setValue(false); // Stop loading on error
                return;
            }

            if (snapshot != null && !snapshot.isEmpty()) {
                List<Post> postsList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    Post post = doc.toObject(Post.class);
                    post.id = doc.getId();
                    postsList.add(post);
                }
                mPosts.setValue(postsList);
            } else {
                mPosts.setValue(new ArrayList<>());
            }
            mIsLoading.setValue(false); // Stop loading when data is received
            if (initialLoad) {
                initialLoad = false;
            }
            Log.d(TAG, "Posts fetched. Loading finished.");
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}