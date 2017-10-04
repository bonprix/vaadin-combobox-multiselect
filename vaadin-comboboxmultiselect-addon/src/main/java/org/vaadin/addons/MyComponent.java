package org.vaadin.addons;

import org.vaadin.addons.client.MyComponentClientRpc;
import org.vaadin.addons.client.MyComponentServerRpc;
import org.vaadin.addons.client.MyComponentState;

import com.vaadin.shared.MouseEventDetails;

// This is the server-side UI component that provides public API 
// for MyComponent
public class MyComponent extends com.vaadin.ui.AbstractComponent {

    private int clickCount = 0;

    public MyComponent() {

        // To receive events from the client, we register ServerRpc
        MyComponentServerRpc rpc = this::handleClick;
        registerRpc(rpc);
    }

    // We must override getState() to cast the state to MyComponentState
    @Override
    protected MyComponentState getState() {
        return (MyComponentState) super.getState();
    }
    
    private void handleClick(MouseEventDetails mouseDetails){
        // Send nag message every 5:th click with ClientRpc
        if (++clickCount % 5 == 0) {
            getRpcProxy(MyComponentClientRpc.class)
                    .alert("Ok, that's enough!");
        }
        
        // Update shared state. This state update is automatically 
        // sent to the client. 
        getState().text = "You have clicked " + clickCount + " times";
    }
}
