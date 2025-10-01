package com.f9ld3.Zion.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class FeedViewModel extends ViewModel {

    // LiveData to hold the list of blog posts
    private final MutableLiveData<List<Post>> mPosts = new MutableLiveData<>();
    public LiveData<List<Post>> getPosts() { return mPosts; }

    // Firebase instances
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration firestoreListener;

    public FeedViewModel() {
        // Start fetching data immediately upon creation
        fetchBlogPosts();
    }

    /**
     * Sets up a real-time listener to fetch and filter documents
     * of type "blog" from the "posts" collection.
     */
    private void fetchBlogPosts() {
        // Query posts collection, filtering for only "blog" types and ordering by creation time
        Query query = db.collection("posts")
                .whereEqualTo("type", "blog")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        firestoreListener = query.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Handle errors
                mPosts.setValue(null);
                return;
            }

            if (snapshot != null && !snapshot.isEmpty()) {
                List<Post> postsList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    // Firestore can automatically map documents to the Post class
                    Post post = doc.toObject(Post.class);
                    // Ensure the document ID is set in the object
                    post.id = doc.getId();
                    postsList.add(post);
                }
                // Update the LiveData with the new list of posts
                mPosts.setValue(postsList);
            } else {
                // If the snapshot is null or empty (no blog posts yet)
                mPosts.setValue(new ArrayList<>());
            }
        });
    }

    /**
     * Clears the Firestore listener when the ViewModel is destroyed
     * to prevent memory leaks and unnecessary network usage.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}