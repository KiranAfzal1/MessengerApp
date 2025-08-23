const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotificationOnMessage = functions.database
  .ref("/chats/{conversationId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    const conversationId = context.params.conversationId;

    if (!message) return null;

    const senderId = message.senderId;
    const text = message.content || "";

    // get conversation meta
    const metaSnap = await admin.database()
      .ref(`/chats/${conversationId}/meta`)
      .once("value");

    const meta = metaSnap.val() || {};
    const participants = meta.participants || [];

    // send to all except sender
    const sendPromises = participants.map(async (uid) => {
      if (uid === senderId) return null;

      const tokenSnap = await admin.database()
        .ref(`/users/${uid}/fcmToken`)
        .once("value");

      const token = tokenSnap.val();
      if (!token) return null;

      const payload = {
        notification: {
          title: "New Message",
          body: text,
        },
        data: {
          conversationId: conversationId,
        },
      };

      return admin.messaging().sendToDevice(token, payload);
    });

    return Promise.all(sendPromises);
  });