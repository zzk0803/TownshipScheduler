package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.ComponentEvent;
import lombok.Getter;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

@Getter
class PlayerFieldFactoryArticleFormFactoryInfoChooseEvent extends ComponentEvent<PlayerFieldFactoryArticleForm> {

    private FieldFactoryInfoEntity fieldFactoryInfoEntity;

    public PlayerFieldFactoryArticleFormFactoryInfoChooseEvent(
            PlayerFieldFactoryArticleForm source,
            boolean fromClient,
            FieldFactoryInfoEntity fieldFactoryInfoEntity
    ) {
        super(source, fromClient);
        this.fieldFactoryInfoEntity = fieldFactoryInfoEntity;
    }

    public PlayerFieldFactoryArticleFormFactoryInfoChooseEvent(PlayerFieldFactoryArticleForm source, boolean fromClient) {
        super(source, fromClient);
    }


}
