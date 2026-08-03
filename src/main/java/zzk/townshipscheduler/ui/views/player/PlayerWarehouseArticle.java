package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.ui.components.ProductsAmountPanel;

@Slf4j
class PlayerWarehouseArticle
        extends Composite<VerticalLayout> {

    private final PlayerViewPresenter playerViewPresenter;

    private ProductsAmountPanel productsAmountPanel;

    public PlayerWarehouseArticle(PlayerViewPresenter playerViewPresenter) {

        this.playerViewPresenter = playerViewPresenter;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("player not exist");
        }
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setSizeFull();
        verticalLayout.setSpacing(false);
        verticalLayout.setMargin(false);
        verticalLayout.setPadding(false);
        return verticalLayout;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.productsAmountPanel = new ProductsAmountPanel(
                this.playerViewPresenter.getCollectionSupplier(),
                this.playerViewPresenter::updateWarehouseStock,
                this.playerViewPresenter.findWarehouseEntityByPlayerEntity().getProductAmountMap()
        );

        HorizontalLayout topWrapper = new HorizontalLayout(
                FlexComponent.JustifyContentMode.END,
                new Button(VaadinIcon.CHECK_CIRCLE.create()) {{
                    addThemeVariants(ButtonVariant.LARGE, ButtonVariant.PRIMARY);
                    addClickListener(_ -> {PlayerWarehouseArticle.this.submit();});
                }}
        ) ;
        VerticalLayout verticalLayout = getContent();
        verticalLayout.removeAll();
        verticalLayout.add(topWrapper);
        verticalLayout.addAndExpand(productsAmountPanel);
        verticalLayout.setFlexShrink(1.0, productsAmountPanel);
    }

    public void submit() {
        this.productsAmountPanel.consume();
    }

}
