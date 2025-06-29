package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.util.Set;

public class ProductDetailPanel extends Composite<VerticalLayout> {

    private ProductEntity productEntity;

    private VerticalLayout materialWrapper;

    private VerticalLayout compositeWrapper;

    public ProductDetailPanel() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }

}
