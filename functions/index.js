const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// ----- CONFIG: apni RTDB region & instance -----
const REGION = "asia-southeast1";
const RTDB_INSTANCE = "messageapp-28a37-default-rtdb"; // URL me jo instance naam hai

// Helper: safe trim
const safePreview = (s = "") => String(s).slice(0, 120);

// ==========================
// Option B (RECOMMENDED): Send to user topic
// App side: user "user_<uid>" topic me subscribed hona chahiye
// ==========================
exports.sendNotificationOnMessage = functions
  .region(REGION)
  .database.instance(RTDB_INSTANCE)
  .ref("/chats/{chatId}/messages/{msgId}")
  .onCreate(async (snapshot, context) => {
    const msg = snapshot.val() || {};
    const chatId = context.params.chatId;

    // meta se participants nikalo
    const metaSnap = await admin.database()
      .ref(`/chats/${chatId}/meta`)
      .once("value");
    const meta = metaSnap.val() || {};
    const participants = meta.participants || [];

    const senderId = msg.senderId;
    if (!senderId || participants.length < 2) {
      console.log("Missing sender or participants.");
      return null;
    }

    // Receiver = jo sender nahi hai
    const receiverId = participants.find((id) => id !== senderId);
    if (!receiverId) return null;

    // Sender ka naam
    const senderNameSnap = await admin.database()
      .ref(`/users/${senderId}/name`)
      .once("value");
    const senderName = senderNameSnap.val() || "New message";

    const payload = {
      notification: {
        title: senderName,
        body: safePreview(msg.content),
      },
      data: {
        type: "chat_message",
        chatId,
        senderId: String(senderId),
      },
    };

    const topic = `user_${receiverId}`;
    try {
      await admin.messaging().sendToTopic(topic, payload);
      console.log("Sent to topic:", topic);
    } catch (e) {
      console.error("FCM sendToTopic error:", e);
    }
    return null;
  });

/*
// ==========================
// Option A: Send directly to saved token
// users/<uid>/fcmToken me token hona chahiye
// ==========================
exports.sendNotificationOnMessageByToken = functions
  .region(REGION)
  .database.instance(RTDB_INSTANCE)
  .ref("/chats/{chatId}/messages/{msgId}")
  .onCreate(async (snapshot, context) => {
    const msg = snapshot.val() || {};
    const chatId = context.params.chatId;

    const metaSnap = await admin.database()
      .ref(`/chats/${chatId}/meta`)
      .once("value");
    const meta = metaSnap.val() || {};
    const participants = meta.participants || [];

    const senderId = msg.senderId;
    const receiverId = participants.find((id) => id !== senderId);
    if (!receiverId) return null;

    const tokenSnap = await admin.database()
      .ref(`/users/${receiverId}/fcmToken`)
      .once("value");
    const token = tokenSnap.val();
    if (!token) {
      console.log("No token for", receiverId);
      return null;
    }

    const senderNameSnap = await admin.database()
      .ref(`/users/${senderId}/name`)
      .once("value");
    const senderName = senderNameSnap.val() || "New message";

    const payload = {
      notification: { title: senderName, body: safePreview(msg.content) },
      data: { type: "chat_message", chatId, senderId: String(senderId) },
    };

    try {
      await admin.messaging().sendToDevice(token, payload);
      console.log("Sent to token:", receiverId);
    } catch (e) {
      console.error("FCM sendToDevice error:", e);
    }
    return null;
  });
*/
