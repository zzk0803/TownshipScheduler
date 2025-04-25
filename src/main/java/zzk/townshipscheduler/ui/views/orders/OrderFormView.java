package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.DateTimeRangeValidator;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.components.BillDurationField;
import zzk.townshipscheduler.ui.components.ProductsCategoriesSelectionPanel;
import zzk.townshipscheduler.ui.eventbus.UiEventBus;
import zzk.townshipscheduler.ui.pojo.BillItem;
import zzk.townshipscheduler.ui.pojo.FactoryProductsDto;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

@Route
@PermitAll
@UIScope
public class OrderFormView extends VerticalLayout {

    private final OrderFormPresenter presenter;

    private final ProductsCategoriesSelectionPanel productsCategoriesSelectionPanel;

    //    private final ProductCategoriesPanel productCategoriesPanel;

    private Grid<BillItem> billItemGrid;

    public OrderFormView(
            OrderFormPresenter orderFormPresenter,
//            ProductCategoriesPanel productCategoriesPanel,
            TownshipAuthenticationContext townshipAuthenticationContext
    ) {
        this.presenter = orderFormPresenter;
//        this.productCategoriesPanel = productCategoriesPanel;
        this.presenter.setOrderFormView(this);
        this.presenter.setTownshipAuthenticationContext(townshipAuthenticationContext);
        this.productsCategoriesSelectionPanel
                = new ProductsCategoriesSelectionPanel(
                presenter.getFactoryProductsSupplier()
        );

        style();

        add(assembleBillForm());

        addAndExpand(assembleBillItemGrid());

        add(assembleItemAppendBtn());

        add(assembleFooterPanel());

        UiEventBus.subscribe(
                this,
                ProductsCategoriesSelectionPanel.ProductCardSelectionAmountEvent.class,
                componentEvent -> {
                    var selectProduct = componentEvent.getProduct();
                    int amount = componentEvent.getAmount();

                    BillItem billItem = new BillItem(
                            presenter.getGridBillItemsCounter().incrementAndGet(),
                            selectProduct,
                            amount
                    );
                    presenter.addBillItem(billItem);
                    presenter.setupDataProviderForItems(billItemGrid);
                }
        );
    }

    private void style() {
        addClassName("bill-form");
        addClassNames(
                LumoUtility.Overflow.SCROLL,
                LumoUtility.Width.FULL
        );
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private FormLayout assembleBillForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.addClassNames("bill-form", "field-form");

        HorizontalLayout deadLineFieldLayout = new HorizontalLayout();
        deadLineFieldLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        Checkbox boolDeadlineCheckbox = new Checkbox("Deadline Given", false);
        BillDurationField durationCountdownField = new BillDurationField();
        DateTimePicker deadlinePicker = new DateTimePicker("Deadline");
        settingDeadlineFieldGroupAvailableStatus(false, durationCountdownField, deadlinePicker);
        associateResponseToPickerAndDuration(deadlinePicker, durationCountdownField);

        boolDeadlineCheckbox.addValueChangeListener(
                valueChange -> settingDeadlineFieldGroupAvailableStatus(
                        valueChange.getValue(),
                        durationCountdownField,
                        deadlinePicker
                )
        );

        deadLineFieldLayout.add(boolDeadlineCheckbox, durationCountdownField, deadlinePicker);

        RadioButtonGroup<String> billTypeGroup = new RadioButtonGroup<>();
        billTypeGroup.setItems(Arrays.stream(OrderType.values()).map(Enum::name).toList());
        billTypeGroup.setValue(OrderType.TRAIN.name());
        billTypeGroup.addValueChangeListener(valueChangeEvent -> {
            String changedIntoValue = valueChangeEvent.getValue();
            if (OrderType.AIRPLANE.name().equalsIgnoreCase(changedIntoValue)) {
                boolDeadlineCheckbox.setValue(true);
                durationCountdownField.setValue(Duration.ofHours(15));
            }
        });

        settingBinder(billTypeGroup, boolDeadlineCheckbox, deadlinePicker, durationCountdownField);

        formLayout.addFormItem(billTypeGroup, "Bill Type");
        formLayout.addFormItem(deadLineFieldLayout, "Duration To Deadline");
        return formLayout;
    }

    private Grid<BillItem> assembleBillItemGrid() {
        Grid<BillItem> grid = new Grid<>(BillItem.class, false);
        grid.setWidthFull();
        grid.addThemeVariants(
                GridVariant.LUMO_NO_ROW_BORDERS,
                GridVariant.LUMO_NO_ROW_BORDERS
        );
        grid.addColumn(BillItem::getSerial)
                .setHeader("#");
        grid.addColumn(buildItemCard())
                .setHeader("Item");
        grid.addComponentColumn(buildItemAmountField())
                .setHeader("Amount Operation");
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        this.billItemGrid = grid;
        return grid;
    }

    private Button assembleItemAppendBtn() {
        Button addItemButton = new Button(VaadinIcon.PLUS.create());
        addItemButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_LARGE
        );
        addItemButton.addClickListener(_ -> {
            Dialog dialog = new Dialog("Select Goods...");
            dialog.setSizeFull();
            dialog.addComponentAsFirst(this.productsCategoriesSelectionPanel);

            Button button = new Button("OK");
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            button.addClickListener(_ -> {
                dialog.close();
                presenter.setupDataProviderForItems(billItemGrid);
            });
            dialog.getFooter().add(button);
            dialog.open();
        });

        return addItemButton;
    }

    private Component assembleFooterPanel() {
        HorizontalLayout footerLayout = new HorizontalLayout();

        Button submit = new Button("Submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(_ -> {
            presenter.onSubmit();
            UI.getCurrent().navigate(OrderListView.class);
        });

        Button cancel = new Button("Cancel");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        cancel.addClickListener(_ -> UI.getCurrent().navigate(OrderListView.class));

        footerLayout.add(submit, cancel);
        footerLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        footerLayout.setAlignItems(Alignment.BASELINE);
        return footerLayout;
    }

    private static void settingDeadlineFieldGroupAvailableStatus(
            boolean boolOpen,
            BillDurationField durationCountdownField,
            DateTimePicker deadlinePicker
    ) {
        durationCountdownField.setEnabled(boolOpen);
        deadlinePicker.setEnabled(boolOpen);
        durationCountdownField.setVisible(boolOpen);
        deadlinePicker.setVisible(boolOpen);
    }

    private void associateResponseToPickerAndDuration(
            DateTimePicker deadlinePicker,
            BillDurationField durationCountdownField
    ) {
        deadlinePicker.addValueChangeListener(deadlinePickerValueChange -> {
            LocalDateTime deadlinePickerValue = deadlinePickerValueChange.getValue();
            Duration duration = getDurationLocalDateTimeConverter().convertToPresentation(deadlinePickerValue, null);
            durationCountdownField.setValue(duration);
        });

        durationCountdownField.addValueChangeListener(durationFieldValueChange -> {
            Duration durationFieldValue = durationFieldValueChange.getValue();
            Result<LocalDateTime> resultInLocalDateTime = getDurationLocalDateTimeConverter().convertToModel(
                    durationFieldValue,
                    null
            );
            LocalDateTime localDateTime = resultInLocalDateTime.getOrThrow(RuntimeException::new);
            deadlinePicker.setValue(localDateTime);
        });
    }

    private void settingBinder(
            RadioButtonGroup<String> billTypeGroup,
            Checkbox boolDeadlineCheckbox,
            DateTimePicker deadlinePicker,
            BillDurationField durationCountdownField
    ) {
        Binder<OrderEntity> binder = presenter.prepareBillAndBinder();
        binder.forField(billTypeGroup)
                .asRequired()
                .bind(
                        orderEntity -> orderEntity.getOrderType().name(),
                        (orderEntity, typeString) -> orderEntity.setOrderType(OrderType.valueOf(typeString))
                );
        binder.forField(boolDeadlineCheckbox)
                .bind(OrderEntity::isBoolDeadLine, OrderEntity::setBoolDeadLine);
        binder.forField(deadlinePicker)
                .withValidator(new DateTimeRangeValidator(
                        "not pasted datetime",
                        LocalDateTime.now(),
                        LocalDateTime.MAX
                ))
                .bind(OrderEntity::getDeadLine, OrderEntity::setDeadLine);
        binder.forField(durationCountdownField)
                .withConverter(getDurationLocalDateTimeConverter())
                .bind(OrderEntity::getDeadLine, OrderEntity::setDeadLine);
    }

    private ComponentRenderer<Div, BillItem> buildItemCard() {
        return new ComponentRenderer<>(billItem -> {
            ProductEntity productEntity = billItem.getProductEntity();

            Div card = new Div();
            card.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.START,
                    LumoUtility.Padding.XSMALL,
                    LumoUtility.Margin.XSMALL
            );

            Image image = new Image();
            image.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.AlignItems.BASELINE,
                    LumoUtility.JustifyContent.START
            );
            image.setSrc(new StreamResource(
                    productEntity.getName(),
                    () -> new ByteArrayInputStream(productEntity.getCrawledAsImage().getImageBytes())
            ));
            card.add(image);

            Div description = createProductDescriptionDiv(productEntity);
            card.add(description);

            return card;
        });
    }

    private ValueProvider<BillItem, IntegerField> buildItemAmountField() {
        return (item) -> {
            IntegerField integerField = new IntegerField();
            integerField.setValue(item.getAmount());
            integerField.setStep(1);
            integerField.setStepButtonsVisible(true);
            integerField.setMin(1);
            integerField.addValueChangeListener(fieldChanged -> {
                item.setAmount(fieldChanged.getValue());
                billItemGrid.getDataProvider().refreshItem(item);
            });
            return integerField;
        };
    }

    private Converter<Duration, LocalDateTime> getDurationLocalDateTimeConverter() {
        return new Converter<>() {

            @Override
            public Result<LocalDateTime> convertToModel(Duration duration, ValueContext valueContext) {
                return Result.ok(LocalDateTime.now().plus(duration));
            }

            @Override
            public Duration convertToPresentation(LocalDateTime localDateTime, ValueContext valueContext) {
                if (localDateTime == null) {
                    return Duration.ZERO;
                }
                return Duration.between(LocalDateTime.now(), localDateTime);
            }
        };
    }

    private static Div createProductDescriptionDiv(ProductEntity productEntity) {
        Div description = new Div();
        description.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.BASELINE,
                LumoUtility.JustifyContent.END,
                LumoUtility.TextColor.SECONDARY
        );
        Span item = new Span("Item:" + productEntity.getName());
        item.addClassNames(LumoUtility.Display.FLEX);
        Span factory = new Span("Factory:" + productEntity.getCategory());
        factory.addClassNames(LumoUtility.Display.FLEX);
        description.add(item, factory);
        return description;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.presenter.setupDataProviderForItems(billItemGrid);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.presenter.clean();
    }

}
