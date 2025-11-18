// main/java/com/f9ld3/Zion/ui/feed/PollViewModel.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel to handle business logic for polls and quizzes, including
 * casting votes and tracking user's vote status and post updates.
 * SIMPLIFIED VERSION - No timestamp updates, batch writes instead of transactions
 */
public class PollViewModel extends ViewModel {
    private static final String TAG = "PollViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Maps to hold LiveData and Listeners
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> userVoteMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> userVoteListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Post>> postDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> postDataListeners = new ConcurrentHashMap<>();

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> getToastMessage() { return _toastMessage; }

    /**
     * Gets a LiveData that holds the index of the option the current user voted for on a given post.
     * Returns -1 if the user has not voted.
     */
    public LiveData<Integer> getUserVoteForPost(String postId) {
        if (postId == null || postId.isEmpty()) {
            Log.w(TAG, "getUserVoteForPost called with invalid postId.");
            return new MutableLiveData<>(-1);
        }
        return userVoteMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Integer> liveData = new MutableLiveData<>(-1);
            startUserVoteListener(id, liveData);
            return liveData;
        });
    }

    /**
     * Gets a LiveData that holds the full Post object for a given post ID.
     */
    public LiveData<Post> getPostData(String postId) {
        if (postId == null || postId.isEmpty()) {
            Log.w(TAG, "getPostData called with invalid postId.");
            return new MutableLiveData<>(null);
        }
        return postDataMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Post> liveData = new MutableLiveData<>(null);
            startPostDataListener(id, liveData);
            return liveData;
        });
    }

    private void startUserVoteListener(String postId, MutableLiveData<Integer> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null || postId.isEmpty()) {
            liveData.postValue(-1);
            return;
        }

        stopUserVoteListener(postId);

        DocumentReference voteRef = db.collection("posts").document(postId)
                .collection("votes").document(user.getUid());

        ListenerRegistration listener = voteRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for user vote on post " + postId, e);
                liveData.postValue(-1);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Long optionIndex = snapshot.getLong("optionIndex");
                liveData.postValue(optionIndex != null ? optionIndex.intValue() : -1);
                Log.d(TAG, "User vote listener updated for " + postId + ": Voted for " + optionIndex);
            } else {
                liveData.postValue(-1);
                Log.d(TAG, "User vote listener updated for " + postId + ": No vote found.");
            }
        });
        userVoteListeners.put(postId, listener);
    }

    private void startPostDataListener(String postId, MutableLiveData<Post> liveData) {
        if (postId == null || postId.isEmpty()) {
            liveData.postValue(null);
            return;
        }

        stopPostDataListener(postId);

        DocumentReference postRef = db.collection("posts").document(postId);

        ListenerRegistration listener = postRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for post data on post " + postId, e);
                liveData.postValue(null);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    Post post = snapshot.toObject(Post.class);
                    if (post != null) {
                        post.setId(snapshot.getId());
                        liveData.postValue(post);
                        Log.d(TAG, "Post data listener updated for " + postId + ". Total Votes: " + post.getTotalVotes());
                    } else {
                        liveData.postValue(null);
                        Log.e(TAG, "Failed to parse Post object for postId: " + postId);
                    }
                } catch (Exception parseError) {
                    liveData.postValue(null);
                    Log.e(TAG, "Exception parsing Post object for postId: " + postId, parseError);
                }
            } else {
                liveData.postValue(null);
                Log.w(TAG, "Post data listener: Document not found for postId: " + postId);
            }
        });
        postDataListeners.put(postId, listener);
    }

    /**
     * Casts a vote for a specific option on a poll/quiz post.
     * SIMPLIFIED: Uses batch write instead of transaction, no timestamp update.
     */
    public void castVote(Post post, int optionIndex) {
        FirebaseUser user = mAuth.getCurrentUser();
        Post currentPostData = postDataMap.containsKey(post.getId()) ? postDataMap.get(post.getId()).getValue() : post;
        if (currentPostData == null) {
            currentPostData = post;
        }

        if (user == null || currentPostData == null || currentPostData.getId() == null) {
            _toastMessage.setValue("You must be logged in to vote.");
            return;
        }

        String postId = currentPostData.getId();
        String userId = user.getUid();

        MutableLiveData<Integer> userVoteLiveData = userVoteMap.computeIfAbsent(postId, id -> new MutableLiveData<>(-1));

        // Check if user already voted (optimistic check)
        if (userVoteLiveData.getValue() != null && userVoteLiveData.getValue() != -1) {
            Log.w(TAG, "Vote attempt ignored: User already voted on post " + postId);
            _toastMessage.setValue("You have already voted on this poll.");
            return;
        }

        if (isPollExpired(currentPostData)) {
            _toastMessage.setValue("This poll has ended.");
            Log.w(TAG, "Vote attempt ignored: Poll expired for post " + postId);
            return;
        }

        // Validate option index
        List<PollOption> options = currentPostData.getPollOptions();
        if (options == null || optionIndex < 0 || optionIndex >= options.size()) {
            _toastMessage.setValue("Invalid option selected.");
            Log.e(TAG, "Invalid option index " + optionIndex + " for post " + postId);
            return;
        }

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference voteRef = postRef.collection("votes").document(userId);

        // Optimistic UI Update
        userVoteLiveData.postValue(optionIndex);
        Log.d(TAG, "Optimistic vote set for post: " + postId + ", index: " + optionIndex);

        // First, check if vote already exists
        voteRef.get().addOnSuccessListener(voteSnapshot -> {
            if (voteSnapshot.exists()) {
                Log.w(TAG, "User already voted on post " + postId);
                _toastMessage.setValue("You have already voted on this poll.");
                userVoteLiveData.postValue(voteSnapshot.getLong("optionIndex").intValue());
                return;
            }

            // Fetch current post data
            postRef.get().addOnSuccessListener(postSnapshot -> {
                if (!postSnapshot.exists()) {
                    Log.e(TAG, "Post not found: " + postId);
                    _toastMessage.setValue("Post not found.");
                    userVoteLiveData.postValue(-1);
                    return;
                }

                Post postFromDb = postSnapshot.toObject(Post.class);
                if (postFromDb == null) {
                    Log.e(TAG, "Failed to parse post: " + postId);
                    _toastMessage.setValue("Error loading post.");
                    userVoteLiveData.postValue(-1);
                    return;
                }
                postFromDb.setId(postSnapshot.getId());

                // Check expiry again with fresh data
                if (isPollExpired(postFromDb)) {
                    _toastMessage.setValue("This poll has ended.");
                    userVoteLiveData.postValue(-1);
                    return;
                }

                // Prepare updated poll options
                List<PollOption> updatedOptions = new ArrayList<>(postFromDb.getPollOptions());
                PollOption selectedOption = updatedOptions.get(optionIndex);
                selectedOption.setVoteCount(selectedOption.getVoteCount() + 1);

                long newTotalVotes = postFromDb.getTotalVotes() + 1;

                // Use batch write for atomic operation
                WriteBatch batch = db.batch();

                // Update post with new vote counts (NO TIMESTAMP)
                Map<String, Object> postUpdates = new HashMap<>();
                postUpdates.put("pollOptions", updatedOptions);
                postUpdates.put("totalVotes", newTotalVotes);
                batch.update(postRef, postUpdates);

                // Create vote document
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("optionIndex", optionIndex);
                voteData.put("votedAt", FieldValue.serverTimestamp());
                batch.set(voteRef, voteData);

                // Commit batch
                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Vote successfully cast for post: " + postId);
                            _toastMessage.setValue("Vote recorded!");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error casting vote for post: " + postId, e);
                            _toastMessage.setValue("Error casting vote. Please try again.");
                            userVoteLiveData.postValue(-1); // Revert optimistic update
                        });

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching post: " + postId, e);
                _toastMessage.setValue("Error loading post.");
                userVoteLiveData.postValue(-1);
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking existing vote: " + postId, e);
            _toastMessage.setValue("Error checking vote status.");
            userVoteLiveData.postValue(-1);
        });
    }

    private void stopUserVoteListener(String postId) {
        if (postId == null || postId.isEmpty()) return;
        ListenerRegistration listener = userVoteListeners.remove(postId);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Removed user vote listener for post: " + postId);
        }
    }

    private void stopPostDataListener(String postId) {
        if (postId == null || postId.isEmpty()) return;
        ListenerRegistration listener = postDataListeners.remove(postId);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Removed post data listener for post: " + postId);
        }
    }

    private boolean isPollExpired(Post post) {
        if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
            return false;
        }
        // --- THIS IS THE FIX ---
        long postTimeMillis = post.getTimestamp().toDate().getTime(); // Convert Timestamp to long
        // --- END FIX ---
        long durationMillis = TimeUnit.HOURS.toMillis(post.getPollDurationHours());
        long expiryTimeMillis = postTimeMillis + durationMillis;
        return System.currentTimeMillis() > expiryTimeMillis;
    }

    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userVoteListeners.values().forEach(ListenerRegistration::remove);
        userVoteListeners.clear();
        postDataListeners.values().forEach(ListenerRegistration::remove);
        postDataListeners.clear();
        userVoteMap.clear();
        postDataMap.clear();
        Log.d(TAG, "PollViewModel cleared, all listeners removed.");
    }
}