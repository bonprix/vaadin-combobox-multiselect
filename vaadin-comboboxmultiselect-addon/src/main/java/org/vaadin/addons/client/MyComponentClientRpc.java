package org.vaadin.addons.client;

import com.vaadin.shared.communication.ClientRpc;

// ClientRpc is used to pass events from server to client
// For sending information about the changes to component state, use State instead
public interface MyComponentClientRpc extends ClientRpc {

    // Example API: Fire up alert box in client
    public void alert(String message);

}