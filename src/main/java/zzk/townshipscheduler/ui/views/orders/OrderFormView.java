package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.DateTimeRangeValidator;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.ui.components.BillDurationField;
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.ProductsAmountPanel;
import zzk.townshipscheduler.ui.pojo.BillItem;
import zzk.townshipscheduler.ui.utility.UiEventBus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Getter
public class OrderFormView extends VerticalLayout {

    private final ProductsAmountPanel productsAmountPanel;

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    private final Binder<OrderEntity> binder = new Binder<>();

    private final List<BillItem> gridBillItems = new ArrayList<>();

    private final LocalDateTime createdDateTime = LocalDateTime.now();

    private AtomicInteger gridBillItemsCounter = new AtomicInteger();

    private OrderEntity orderEntity;

    private ListDataProvider<BillItem> billItemGridDataProvider;

    private GridListDataView<BillItem> billItemGridListDataView;

    private Grid<BillItem> billItemGrid;

    public OrderFormView(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository,
            TownshipAuthenticationContext townshipAuthenticationContext
    ) {

        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.fieldFactoryInfoEntityRepository = fieldFactoryInfoEntityRepository;
        this.townshipAuthenticationContext = townshipAuthenticationContext;
        this.productsAmountPanel
                = new ProductsAmountPanel(() -> {
            Optional<PlayerEntity> playerEntity = townshipAuthenticationContext.getPlayerEntity();
            return playerEntity.<Supplier<Collection<FieldFactoryInfoEntity>>>map(
                    entity -> () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                            entity.getLevel(),
                            Sort.by(Sort.Direction.ASC, "level")
                    )).orElseGet(() -> () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                    Sort.by(Sort.Direction.ASC, "level")
            )).get();
        });

        style();
        add(assembleBillForm());
        addAndExpand(this.billItemGrid = assembleBillItemGrid());
        add(assembleItemAppendBtn());
        add(assembleFooterPanel());

        UiEventBus.subscribe(
                this,
                ProductsAmountPanel.ProductCardSelectionAmountEvent.class,
                componentEvent -> {
                    var selectProduct = componentEvent.getProduct();
                    int amount = componentEvent.getAmount();

                    BillItem billItem = new BillItem(
                            getGridBillItemsCounter().incrementAndGet(),
                            selectProduct,
                            amount
                    );
                    addBillItem(billItem);
                    setupDataProviderForItems(this.billItemGrid);
                }
        );
        this.setupDataProviderForItems(this.billItemGrid);
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

        RadioButtonGroup<OrderType> billTypeGroup = new RadioButtonGroup<>();
        billTypeGroup.setItems(OrderType.values());
        billTypeGroup.setValue(OrderType.TRAIN);
        billTypeGroup.setItemLabelGenerator(Enum::name);
        billTypeGroup.addValueChangeListener(valueChangeEvent -> {
            if (OrderType.AIRPLANE == valueChangeEvent.getValue()) {
                boolDeadlineCheckbox.setValue(true);
                durationCountdownField.setValue(Duration.ofHours(15));
            }
        });

        settingBinder(billTypeGroup, boolDeadlineCheckbox, deadlinePicker);

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
            dialog.addComponentAsFirst(this.productsAmountPanel);

            Button button = new Button("OK");
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            button.addClickListener(_ -> {
                dialog.close();
                setupDataProviderForItems(billItemGrid);
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
            onSubmit();
            UiEventBus.publish(
                    new OrderFormViewHasSubmitEvent(
                            this,
                            false
                    )
            );
        });

        Button cancel = new Button("Cancel");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        cancel.addClickListener(_ -> UI.getCurrent().navigate(OrderListView.class));

        footerLayout.add(submit, cancel);
        footerLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        footerLayout.setAlignItems(Alignment.BASELINE);
        return footerLayout;
    }

    private void addBillItem(BillItem billItem) {
        getGridBillItems().stream()
                .filter(iterating -> iterating.getProductEntity()
                        .getProductId()
                        .equals(billItem.getProductEntity().getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        optionalPresent -> {
                            int amount = optionalPresent.getAmount();
                            optionalPresent.setAmount(amount);
                        }, () -> getGridBillItems().add(billItem)
                );
    }

    private void setupDataProviderForItems(Grid<BillItem> grid) {
        billItemGridDataProvider = new ListDataProvider<>(this.gridBillItems);
        billItemGridListDataView = grid.setItems(this.billItemGridDataProvider);
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
            RadioButtonGroup<OrderType> billTypeGroup,
            Checkbox boolDeadlineCheckbox,
            DateTimePicker deadlinePicker
    ) {
        renewBinderAndObject();
        this.binder.forField(billTypeGroup)
                .asRequired()
                .bind(
                        OrderEntity::getOrderType,
                        OrderEntity::setOrderType
                );
        this.binder.forField(boolDeadlineCheckbox)
                .bind(OrderEntity::isBearDeadline, OrderEntity::setBearDeadline);
        this.binder.forField(deadlinePicker)
                .withValidator(new DateTimeRangeValidator(
                        "not pasted datetime",
                        LocalDateTime.now(),
                        LocalDateTime.MAX
                ))
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

            Image image =ProductImages.productImage(
                    productEntity.getName(),
                    productEntity.getCrawledAsImage().getImageBytes()
            );
            image.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.AlignItems.BASELINE,
                    LumoUtility.JustifyContent.START
            );
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

    private void onSubmit() {
        try {
            this.orderEntity.setCreatedDateTime(createdDateTime);
            if (!this.orderEntity.isBearDeadline()) {
                this.orderEntity.setDeadLine(null);
            }

            getGridBillItems().forEach(
                    billItem -> this.orderEntity.addItem(
                            billItem.getProductEntity(),
                            billItem.getAmount()
                    )
            );

            Optional.ofNullable(getTownshipAuthenticationContext())
                    .map(TownshipAuthenticationContext::getUserDetails)
                    .map(AccountEntity::getPlayerEntity)
                    .ifPresent(player -> {
                        orderEntity.setPlayerEntity(player);
                    });

            getBinder().writeBean(this.orderEntity);
            getOrderEntityRepository().saveAndFlush(this.orderEntity);
        } catch (ValidationException e) {
            Notification.show(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Converter<Duration, LocalDateTime> getDurationLocalDateTimeConverter() {
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

    private void renewBinderAndObject() {
        this.binder.readBean(this.orderEntity = new OrderEntity());
    }

    private Div createProductDescriptionDiv(ProductEntity productEntity) {
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

    public static class OrderFormViewHasSubmitEvent extends ComponentEvent<OrderFormView> {

        public OrderFormViewHasSubmitEvent(OrderFormView source, boolean fromClient) {
            super(source, fromClient);
        }

    }

}
