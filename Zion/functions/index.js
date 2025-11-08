// Import the necessary Firebase modules
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

// *** MODIFIED LINE ***
// Explicitly initialize with your Project ID
initializeApp({
  projectId: "zion-31e23"
});
// *** END MODIFIED LINE ***


/**
 * Cloud Function that triggers when a new document is created in the
 * 'notifications' collection using the v2 syntax.
 */
exports.sendPushNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {

    // 1. Get the new notification data from the document
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event");
        return;
    }
    const notificationData = snapshot.data();

    const targetUserId = notificationData.targetUserId;
    const title = notificationData.title;
    const body = notificationData.message;

    // Get the 'data' map you created in your app
    const dataPayload = notificationData.data || {};

    // Ensure the 'type' is also in the data payload for the client
    dataPayload.type = notificationData.type || '';

    if (!targetUserId) {
        console.error("No targetUserId in notification doc:", snapshot.id);
        return;
    }

    // 2. Get the target user's FCM tokens
    // This path matches what your MyFirebaseMessagingService.java uses
    const tokensSnapshot = await getFirestore()
        .collection('users').doc(targetUserId)
        .collection('fcmTokens').get();

    if (tokensSnapshot.empty) {
        console.warn("No FCM tokens found for user:", targetUserId);
        return;
    }

    // Get all token strings
    const tokens = tokensSnapshot.docs.map(doc => doc.id);

    // 3. Create the FCM message payload
    const payload = {
        notification: {
            title: title,
            body: body,
        },
        data: dataPayload // This sends your data (postId, commentId, etc.)
    };

    // 4. Send the message to all the user's devices
    console.log("Sending FCM message to tokens:", tokens);
    const response = await getMessaging().sendToDevice(tokens, payload);

    // 5. (Optional) Clean up stale tokens
    const tokensToRemove = [];
    response.results.forEach((result, index) => {
        const error = result.error;
        if (error) {
            console.error('Failure sending notification to', tokens[index], error);
            // Check for errors indicating a bad token
            if (error.code === 'messaging/invalid-registration-token' ||
                error.code === 'messaging/registration-token-not-registered') {
                // Schedule the token for deletion
                tokensToRemove.push(tokensSnapshot.docs[index].ref.delete());
            }
        }
    });

    // Delete any invalid tokens
    return Promise.all(tokensToRemove);
});