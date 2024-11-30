package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.ComponentEvent;

class PlayerFieldFactoryPersistRequestEvent extends ComponentEvent<PlayerFieldFactoryArticle> {

    public PlayerFieldFactoryPersistRequestEvent(PlayerFieldFactoryArticle source, boolean fromClient) {
        super(source, fromClient);
    }

}
