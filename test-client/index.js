const DEBUG = true;
const tenantStomps = {};
const scopes = ["news", "alerts", "chat"];

function debug(...args) {
  if (DEBUG) {
    console.log("[DEBUG]", ...args);
  }
}

function updateConnectionStatus(tenant, isConnected) {
  const statusElement = document.getElementById(`${tenant}-status`);
  if (statusElement) {
    statusElement.textContent = `${tenant}: ${
      isConnected ? "Connected" : "Disconnected"
    }`;
    statusElement.className = isConnected ? "connected" : "disconnected";
  }
}

function connect(tenant) {
  if (tenantStomps[tenant]) {
    debug(`Disconnecting existing connection for ${tenant}`);
    tenantStomps[tenant].stomp.disconnect();
    delete tenantStomps[tenant];
    updateConnectionStatus(tenant, false);
  }

  const userId = `user_${Math.random().toString(36).substring(2, 9)}`;
  debug(`Connecting ${tenant} with userId ${userId}`);

  const socket = new SockJS("http://localhost:8085/ws");
  const stomp = Stomp.over(socket);

  stomp.debug = DEBUG ? console.log : null;

  stomp.connect(
    { Authorization: `Bearer ${tenant}` },
    (frame) => {
      debug(`Connected to ${tenant}:`, frame);
      tenantStomps[tenant] = { stomp, userId };
      updateConnectionStatus(tenant, true);
      scopes.forEach((scope) => {
        debug(`Subscribing to /topic/${scope} for ${tenant}`);
        stomp.subscribe(
          `/topic/${scope}`,
          function (response) {
            debug(`=== Subscription callback for ${scope} ===`);
            debug("Raw response:", response);
            try {
              const rawBody = response.body;
              debug("Raw body:", rawBody);
              const message = JSON.parse(rawBody);
              debug("Parsed message:", message);

              if (message.tenant === tenant) {
                debug(`Message accepted for tenant ${tenant}`);
                appendMessage(tenant, message.scope, message);
              } else {
                debug(
                  `Message rejected - wrong tenant (got ${message.tenant}, expected ${tenant})`
                );
              }
            } catch (error) {
              console.error("Error in subscription callback:", error);
              console.error("Response that caused error:", response);
            }
            debug(`=== End subscription callback for ${scope} ===`);
          },
          {
            ack: "auto",
            id: `sub-${scope}-${tenant}`,
            durable: false,
            exclusive: false,
          }
        );
        debug(`Subscribed to /topic/${scope}`);
      });
    },
    (error) => {
      console.error(`Connection error for ${tenant}:`, error);
      updateConnectionStatus(tenant, false);
      appendMessage(tenant, "alerts", {
        content: `Connection error: ${error}`,
        timestamp: new Date(),
      });
    }
  );
}

function sendManualMessage(tenant) {
  const connection = tenantStomps[tenant];
  if (!connection || !connection.stomp) {
    console.error(`No connection found for tenant ${tenant}`);
    return;
  }

  const messageInput = document.getElementById(`${tenant}-message`);
  const scopeSelect = document.getElementById(`${tenant}-scope`);

  if (!messageInput.value) {
    console.warn("Message content is empty");
    return;
  }

  const message = {
    content: messageInput.value,
    tenant: tenant,
    userId: connection.userId,
    scope: scopeSelect.value,
    timestamp: new Date(),
  };

  try {
    debug("Sending message:", message);
    connection.stomp.send(`/app/send/${message.scope}`, {}, JSON.stringify(message));
    messageInput.value = "";
  } catch (error) {
    console.error("Error sending message:", error);
  }
}

function appendMessage(tenant, scope, message) {
  debug(`Appending message: ${JSON.stringify(message)} to ${tenant}-${scope}`);
  const messageBox = document.getElementById(`${tenant}-${scope}`);
  if (!messageBox) {
    console.error(`Message box not found: ${tenant}-${scope}`);
    return;
  }

  const messageElement = document.createElement("div");
  const time = new Date().toLocaleTimeString();
  messageElement.textContent = `${time} - ${message.content}`;

  if (message.content.startsWith("ACK:")) {
    messageElement.style.color = "green";
  } else if (message.content.startsWith("ECHO:")) {
    messageElement.style.color = "blue";
  }

  messageBox.appendChild(messageElement);
  messageBox.scrollTop = messageBox.scrollHeight;
  debug(`Successfully appended message to ${tenant}-${scope}`);
}
