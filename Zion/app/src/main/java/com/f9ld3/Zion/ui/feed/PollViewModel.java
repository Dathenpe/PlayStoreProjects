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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewModel to handle business logic for polls and quizzes, including
 * casting votes and tracking user's vote status.
 */
public class PollViewModel extends ViewModel {
    private static final String TAG = "PollViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final ConcurrentHashMap<String, MutableLiveData<Integer>> userVoteMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> voteListeners = new ConcurrentHashMap<>();

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> getToastMessage() { return _toastMessage; }

    /**
     * Gets a LiveData that holds the index of the option the current user voted for on a given post.
     * Returns -1 if the user has not voted.
     * @param postId The ID of the post.
     * @return LiveData<Integer> representing the user's voted option index.
     */
    public LiveData<Integer> getUserVoteForPost(String postId) {
        return userVoteMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Integer> liveData = new MutableLiveData<>(-1); // Default to -1 (no vote)
            startVoteListener(id, liveData);
            return liveData;
        });
    }

    private void startVoteListener(String postId, MutableLiveData<Integer> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            liveData.postValue(-1);
            return;
        }

        stopVoteListener(postId);

        DocumentReference voteRef = db.collection("posts").document(postId)
                .collection("votes").document(user.getUid());

        ListenerRegistration listener = voteRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for vote on post " + postId, e);
                liveData.postValue(-1);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Long optionIndex = snapshot.getLong("optionIndex");
                liveData.postValue(optionIndex != null ? optionIndex.intValue() : -1);
            } else {
                liveData.postValue(-1);
            }
        });
        voteListeners.put(postId, listener);
    }

    /**
     * Casts a vote for a specific option on a poll/quiz post using a transaction.
     * @param post The Post object being voted on.
     * @param optionIndex The index of the selected option.
     */
    public void castVote(Post post, int optionIndex) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _toastMessage.setValue("You must be logged in to vote.");
            return;
        }

        LiveData<Integer> userVote = getUserVoteForPost(post.getId());
        if (userVote.getValue() != null && userVote.getValue() != -1) {
            _toastMessage.setValue("You have already voted on this poll.");
            return;
        }

        String userId = user.getUid();
        String postId = post.getId();
        DocumentReference postRef = db.collection("posts").document(postId);

        db.runTransaction(transaction -> {
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            Post postFromDb = postSnapshot.toObject(Post.class);
            if (postFromDb == null) {
                try {
                    throw new Exception("Post not found");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            List<PollOption> options = postFromDb.getPollOptions();
            if (optionIndex >= 0 && optionIndex < options.size()) {
                PollOption selectedOption = options.get(optionIndex);
                selectedOption.setVoteCount(selectedOption.getVoteCount() + 1);
            } else {
                try {
                    throw new Exception("Invalid option index");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            long newTotalVotes = postFromDb.getTotalVotes() + 1;

            transaction.update(postRef, "pollOptions", options);
            transaction.update(postRef, "totalVotes", newTotalVotes);

            // Also record the user's specific vote in the subcollection
            DocumentReference userVoteDocRef = postRef.collection("votes").document(user.getUid());
            Map<String, Object> newVoteData = new HashMap<>();
            newVoteData.put("optionIndex", optionIndex);
            newVoteData.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(userVoteDocRef, newVoteData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Vote successfully cast for post: " + postId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error casting vote for post: " + postId, e);
            _toastMessage.setValue("Error casting vote.");
        });
    }


    private void stopVoteListener(String postId) {
        ListenerRegistration listener = voteListeners.remove(postId);
        if (listener != null) {
            listener.remove();
        }
    }

    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        voteListeners.values().forEach(ListenerRegistration::remove);
        voteListeners.clear();
        userVoteMap.clear();
    }
}
