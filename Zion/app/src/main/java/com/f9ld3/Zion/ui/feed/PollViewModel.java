// main/java/com/f9ld3/Zion/ui/feed/PollViewModel.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp; // Import Timestamp
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
// Removed WriteBatch import as transaction is used
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit; // Import TimeUnit

/**
 * ViewModel to handle business logic for polls and quizzes, including
 * casting votes and tracking user's vote status and post updates.
 */
public class PollViewModel extends ViewModel {
    private static final String TAG = "PollViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // *** Maps to hold LiveData and Listeners ***
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> userVoteMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> userVoteListeners = new ConcurrentHashMap<>();
    // *** NEW: Map for Post LiveData and Listeners ***
    private final ConcurrentHashMap<String, MutableLiveData<Post>> postDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> postDataListeners = new ConcurrentHashMap<>();

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> getToastMessage() { return _toastMessage; }

    /**
     * Gets a LiveData that holds the index of the option the current user voted for on a given post.
     * Returns -1 if the user has not voted.
     * @param postId The ID of the post.
     * @return LiveData<Integer> representing the user's voted option index.
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
     * This allows observing changes like vote counts.
     * @param postId The ID of the post.
     * @return LiveData<Post> holding the latest post data.
     */
    // *** NEW: Method to get LiveData for the Post itself ***
    public LiveData<Post> getPostData(String postId) {
        if (postId == null || postId.isEmpty()) {
            Log.w(TAG, "getPostData called with invalid postId.");
            return new MutableLiveData<>(null); // Return null for invalid ID
        }
        return postDataMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Post> liveData = new MutableLiveData<>(null); // Start with null
            startPostDataListener(id, liveData);
            return liveData;
        });
    }
    // *** END NEW ***

    // *** Renamed from startVoteListener ***
    private void startUserVoteListener(String postId, MutableLiveData<Integer> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null || postId.isEmpty()) {
            liveData.postValue(-1);
            return;
        }

        stopUserVoteListener(postId); // Use specific stop method

        DocumentReference voteRef = db.collection("posts").document(postId)
                .collection("votes").document(user.getUid());

        // Listen specifically to the user's vote document
        ListenerRegistration listener = voteRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for user vote on post " + postId, e);
                liveData.postValue(-1); // Assume no vote on error
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Long optionIndex = snapshot.getLong("optionIndex");
                liveData.postValue(optionIndex != null ? optionIndex.intValue() : -1);
                Log.d(TAG, "User vote listener updated for " + postId + ": Voted for " + (optionIndex != null ? optionIndex : "null"));
            } else {
                liveData.postValue(-1); // No vote document found
                Log.d(TAG, "User vote listener updated for " + postId + ": No vote found.");
            }
        });
        userVoteListeners.put(postId, listener); // Store in user vote listeners map
    }

    // *** NEW: Listener for the Post document itself ***
    private void startPostDataListener(String postId, MutableLiveData<Post> liveData) {
        if (postId == null || postId.isEmpty()) {
            liveData.postValue(null);
            return;
        }

        stopPostDataListener(postId); // Use specific stop method

        DocumentReference postRef = db.collection("posts").document(postId);

        // Listen to the main post document for changes (like counts)
        ListenerRegistration listener = postRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for post data on post " + postId, e);
                // Don't necessarily clear LiveData, maybe keep stale data? Or post null.
                liveData.postValue(null); // Post null on error
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    Post post = snapshot.toObject(Post.class);
                    if (post != null) {
                        post.setId(snapshot.getId()); // Ensure ID is set
                        liveData.postValue(post); // Update LiveData with the full Post object
                        Log.d(TAG, "Post data listener updated for " + postId + ". Total Votes: " + post.getTotalVotes());
                    } else {
                        liveData.postValue(null); // Parsing failed
                        Log.e(TAG, "Failed to parse Post object for postId: " + postId);
                    }
                } catch (Exception parseError) {
                    liveData.postValue(null); // Parsing failed
                    Log.e(TAG, "Exception parsing Post object for postId: " + postId, parseError);
                }
            } else {
                liveData.postValue(null); // Post doesn't exist
                Log.w(TAG, "Post data listener: Document not found for postId: " + postId);
            }
        });
        postDataListeners.put(postId, listener); // Store in post data listeners map
    }
    // *** END NEW ***

    /**
     * Casts a vote for a specific option on a poll/quiz post using a transaction.
     * @param post The Post object being voted on (can be slightly stale, transaction re-fetches).
     * @param optionIndex The index of the selected option.
     */
    public void castVote(Post post, int optionIndex) {
        FirebaseUser user = mAuth.getCurrentUser();
        // Use postDataMap to get potentially more up-to-date post object if available
        Post currentPostData = postDataMap.containsKey(post.getId()) ? postDataMap.get(post.getId()).getValue() : post;
        // If the live data is null, fall back to the passed post object
        if (currentPostData == null) {
            currentPostData = post;
        }

        if (user == null || currentPostData == null || currentPostData.getId() == null) {
            _toastMessage.setValue("You must be logged in to vote.");
            return;
        }

        String postId = currentPostData.getId();
        LiveData<Integer> userVote = getUserVoteForPost(postId);

        if (userVote.getValue() != null && userVote.getValue() != -1) {
            Log.w(TAG, "Vote attempt ignored: User already voted on post " + postId);
            return;
        }

        if (isPollExpired(currentPostData)) { // Use potentially updated post data for check
            _toastMessage.setValue("This poll has ended.");
            Log.w(TAG, "Vote attempt ignored: Poll expired for post " + postId);
            // Force UI update by reloading the vote status
            startUserVoteListener(postId, (MutableLiveData<Integer>) userVote); // << Use startUserVoteListener
            return;
        }

        String userId = user.getUid();
        DocumentReference postRef = db.collection("posts").document(postId);

        // *** Transaction remains largely the same ***
        db.runTransaction(transaction -> {
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            Post postFromDb = postSnapshot.exists() ? postSnapshot.toObject(Post.class) : null;
            if (postFromDb == null) {
                throw new RuntimeException("Post not found during transaction: " + postId);
            }
            // Add ID manually as it's not stored in the document fields
            postFromDb.setId(postSnapshot.getId());

            if (isPollExpired(postFromDb)) {
                throw new RuntimeException("Poll expired during transaction: " + postId);
            }

            // Check if user already voted within transaction
            DocumentReference userVoteDocRefCheck = postRef.collection("votes").document(user.getUid());
            DocumentSnapshot userVoteSnapshot = transaction.get(userVoteDocRefCheck);
            if (userVoteSnapshot.exists()) {
                Log.w(TAG, "Transaction check: User " + userId + " already voted on " + postId);
                return null; // Vote already registered, exit transaction gracefully
            }

            List<PollOption> options = postFromDb.getPollOptions();
            if (options == null || optionIndex < 0 || optionIndex >= options.size()) {
                throw new RuntimeException("Invalid option index: " + optionIndex + " for post " + postId);
            }

            // --- Safely modify the options list fetched within the transaction ---
            // Create a mutable copy if necessary, or modify directly if Firestore allows
            List<PollOption> updatedOptions = new ArrayList<>(options); // Create a mutable copy
            PollOption selectedOption = updatedOptions.get(optionIndex); // Get from the copy
            selectedOption.setVoteCount(selectedOption.getVoteCount() + 1); // Increment vote count
            // --- End Safe Modification ---

            long newTotalVotes = postFromDb.getTotalVotes() + 1;

            transaction.update(postRef, "pollOptions", updatedOptions); // Update with the modified copy
            transaction.update(postRef, "totalVotes", newTotalVotes);

            DocumentReference userVoteDocRef = postRef.collection("votes").document(user.getUid());
            Map<String, Object> newVoteData = new HashMap<>();
            newVoteData.put("optionIndex", optionIndex);
            newVoteData.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(userVoteDocRef, newVoteData);

            return null; // Indicate success
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Vote transaction successful for post: " + postId);
            // UI will update via the postDataListener now
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error casting vote transaction for post: " + postId, e);
            if (e.getMessage() != null && e.getMessage().contains("Poll expired")) {
                _toastMessage.setValue("This poll has ended.");
            } else {
                _toastMessage.setValue("Error casting vote. Please try again.");
            }
            // Force UI update in case of failure (listeners might need refresh)
            startUserVoteListener(postId, (MutableLiveData<Integer>) userVote); // << Use startUserVoteListener
            // Optionally, also force refresh the post data listener
            startPostDataListener(postId, postDataMap.get(postId));
        });
    }


    // *** Renamed from stopVoteListener ***
    private void stopUserVoteListener(String postId) {
        if (postId == null || postId.isEmpty()) return;
        ListenerRegistration listener = userVoteListeners.remove(postId);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Removed user vote listener for post: " + postId);
        }
    }

    // *** NEW: Method to stop Post Data Listener ***
    private void stopPostDataListener(String postId) {
        if (postId == null || postId.isEmpty()) return;
        ListenerRegistration listener = postDataListeners.remove(postId);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Removed post data listener for post: " + postId);
        }
    }
    // *** END NEW ***

    private boolean isPollExpired(Post post) {
        if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
            return false;
        }
        long postTimeMillis = post.getTimestamp();
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
        // Remove all listeners when ViewModel is destroyed
        userVoteListeners.values().forEach(ListenerRegistration::remove);
        userVoteListeners.clear();
        postDataListeners.values().forEach(ListenerRegistration::remove); // <<< Clear post listeners
        postDataListeners.clear(); // <<< Clear post listeners map

        userVoteMap.clear();
        postDataMap.clear(); // <<< Clear post data map
        Log.d(TAG, "PollViewModel cleared, all listeners removed.");
    }
}