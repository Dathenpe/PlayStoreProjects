// main/java/com/f9ld3/Zion/ui/feed/FeedViewModel.java
package com.f9ld3.Zion.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException; // Import Exception

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
            // Handle Firestore errors
            if (e != null) {
                Log.e(TAG, "Error fetching posts", e); // Log the specific error
                mPosts.setValue(new ArrayList<>()); // Set empty list on error
                mIsLoading.setValue(false); // Stop loading on error
                // Optionally, you could expose the error message to the UI via another LiveData
                return;
            }

            // Process the snapshot
            if (snapshot != null && !snapshot.isEmpty()) {
                List<Post> postsList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    try {
                        // --- FIX: Add try-catch around toObject ---
                        Post post = doc.toObject(Post.class);
                        post.setId(doc.getId()); // Set the document ID here
                        postsList.add(post);
                        // --- END FIX ---
                    } catch (RuntimeException parseError) {
                        // Log the error if a specific document fails to parse
                        Log.e(TAG, "Error parsing post document: " + doc.getId() + ". Skipping this post.", parseError);
                        // Optionally notify the user or track this error
                    }
                }
                mPosts.setValue(postsList); // Update LiveData with successfully parsed posts
                Log.d(TAG, "Successfully processed " + postsList.size() + " posts out of " + snapshot.size());
            } else {
                Log.d(TAG, "Post snapshot is null or empty.");
                mPosts.setValue(new ArrayList<>()); // Set empty list if snapshot is empty
            }

            mIsLoading.setValue(false); // Stop loading when data is received or processed
            if (initialLoad) {
                initialLoad = false;
            }
            Log.d(TAG, "Posts fetch listener update processed. Loading finished.");
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (firestoreListener != null) {
            firestoreListener.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
    }
}