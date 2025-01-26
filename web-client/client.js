const connections = {};
const userIds = {};

function connect(tenant) {
  if (connections[tenant]) return;

  const randomUserId = Math.random().toString(36).substring(2, 10);
  userIds[tenant] = randomUserId;

  const socket = new SockJS("http://localhost:8080/ws");
  const client = Stomp.over(socket);

  client.heartbeat = { outgoing: 10000, incoming: 10000 };

  const connectHeaders = {
    authorization: `Bearer ${tenant}`,
    "user-id": "guest",
    "custom-user-id": randomUserId,
    "accept-version": "1.1,1.0",
    "heart-beat": "10000,10000",
  };

  let reconnectAttempts = 0;
  const maxReconnectAttempts = 5;
  const reconnectDelay = 5000;

  const connectWithRetry = () => {
    client.connect(
      connectHeaders,
      (frame) => {
        reconnectAttempts = 0;
        connections[tenant] = client;
        document.getElementById(
          `userid-${tenant}`
        ).textContent = `User ID: ${randomUserId}`;
        updateStatus(tenant, true);

        ["news", "alert", "chat"].forEach((topic) => {
          client.subscribe(`/topic/${topic}`, (message) => {
            displayMessage(
              tenant,
              topic,
              message.body,
              message.headers["custom-user-id"]
            );
          });
        });
      },
      (error) =>
        handleConnectionError(
          tenant,
          error,
          reconnectAttempts++,
          maxReconnectAttempts,
          reconnectDelay,
          connectWithRetry
        )
    );
  };

  connectWithRetry();
}

function handleConnectionError(
  tenant,
  error,
  attempts,
  maxAttempts,
  delay,
  retryFn
) {
  console.error(`Error connecting to ${tenant}:`, error);

  if (attempts < maxAttempts) {
    console.log(`Attempting to reconnect (${attempts}/${maxAttempts})...`);
    setTimeout(retryFn, delay);
  } else {
    console.error(`Failed to reconnect after ${maxAttempts} attempts`);
    cleanup(tenant);
  }
}

function displayMessage(tenant, topic, text, senderId) {
  const messageDiv = document.createElement("div");
  messageDiv.className = `message ${
    senderId === userIds[tenant] ? "outgoing" : "incoming"
  }`;

  const timestamp = new Date().toLocaleTimeString();
  messageDiv.textContent = `[${timestamp}] ${
    senderId ? "User " + senderId.substr(0, 8) : "Unknown"
  }: ${text}`;

  document
    .getElementById(`messages-${tenant}-${topic}`)
    .appendChild(messageDiv);
}

function updateStatus(tenant, isConnected) {
  const statusEl = document.getElementById(`status-${tenant}`);
  statusEl.textContent = isConnected ? "Connected" : "Disconnected";
  statusEl.className = `badge ${isConnected ? "badge-success" : "badge-error"}`;
}

function cleanup(tenant) {
  delete connections[tenant];
  delete userIds[tenant];
  updateStatus(tenant, false);
  document.getElementById(`userid-${tenant}`).textContent = "";
}

function disconnect(tenant) {
  if (connections[tenant]) {
    connections[tenant].disconnect();
    cleanup(tenant);
  }
}

function sendMessage(tenant, topic) {
  const client = connections[tenant];
  if (!client) {
    alert("Not connected! Please connect first.");
    return;
  }

  const messageInput = document.getElementById(`message-${tenant}-${topic}`);
  const message = messageInput.value;

  if (message) {
    client.send(
      `/app/send/${topic}`,
      {
        authorization: `Bearer ${tenant}`,
        "user-id": "guest",
        "custom-user-id": userIds[tenant],
      },
      message
    );

    messageInput.value = "";
  }
}

window.onbeforeunload = () => Object.keys(connections).forEach(disconnect);
