const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNewMessageNotification = functions.database
  .ref("/chats/{conversationId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    const conversationId = context.params.conversationId;
    const senderId = message.senderId;
    const text = message.content;

    const metaSnap = await admin.database().ref(`/chats/${conversationId}/meta`).once("value");
    const participants = metaSnap.val()?.participants || [];

    for (const uid of participants) {
      if (uid === senderId) continue; // skip sender
      const tokenSnap = await admin.database().ref(`/users/${uid}/fcm_token`).once("value");
      const token = tokenSnap.val();
      if (!token) continue;

      const payload = {
        notification: {
          title: "New message",
          body: text
        }
      };

      await admin.messaging().sendToDevice(token, payload);
    }
  });
