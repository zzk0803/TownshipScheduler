package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.DateTimeRangeValidator;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.ui.components.BillDurationField;
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.ProductsAmountPanel;
import zzk.townshipscheduler.ui.pojo.BillItem;

import java.io.Serial;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Getter
public class OrderFormView
        extends VerticalLayout {

    @Serial
    private static final long serialVersionUID = -2333101928692705488L;

    private final OrderListView orderListView;

    private final OrderListViewPresenter orderListViewPresenter;

    private final ProductsAmountPanel productsAmountPanel;

    private final Binder<OrderEntity> binder = new Binder<>();

    private final AtomicInteger gridBillItemsCounter = new AtomicInteger(1);

    private final ValueSignal<Boolean> orderDeadlineSignal = new ValueSignal<>(false);

    private final ValueSignal<Duration> deadlineDurationSignal = new ValueSignal<>(Duration.ZERO);

    private final ValueSignal<LocalDateTime> orderReleaseDateTime = new ValueSignal<>(LocalDateTime.now());

    private final ValueSignal<OrderType> orderTypeValueSignal = new ValueSignal<>(OrderType.TRAIN);

    private final Signal<LocalDateTime> orderDueDateTime = Signal.computed(
            () -> {
                LocalDateTime localDateTime = orderReleaseDateTime.get();
                Duration duration = deadlineDurationSignal.get();
                return localDateTime.plus(duration);
            });

    private final ListSignal<BillItem> billItemListSignal = new ListSignal<>();

    private final Grid<BillItem> billItemGrid;

    private final boolean editMode;

    private OrderEntity orderEntity;

    private OrderEntity editModeOrderEntity;

    private ScheduledFuture<?> scheduledFuture;

    public OrderFormView(
            OrderListView orderListView,
            OrderListViewPresenter orderListViewPresenter,
            AtomicReference<Dialog> dialogReference,
            OrderEntity editModeOrderEntity
    ) {
        this.orderListView = orderListView;
        this.orderListViewPresenter = orderListViewPresenter;
        this.editModeOrderEntity = editModeOrderEntity;
        this.editMode = true;
        this.productsAmountPanel = new ProductsAmountPanel(
                this.orderListViewPresenter.getCollectionSupplier(),
                billItemListSignalConsumer(),
                this.getEditModeOrderEntity().toProductAmountMap()
        );
        this.billItemListSignalConsumer().accept(this.getEditModeOrderEntity().toProductAmountMap());

        style();
        add(assembleBillForm());
        addAndExpand(this.billItemGrid = assembleBillItemGrid());
        add(assembleItemAppendBtn());
        add(assembleFooterPanel(dialogReference));

    }

    private @NonNull Consumer<Map<ProductEntity, Integer>> billItemListSignalConsumer() {
        if (editMode) {
            return productEntityIntegerMap -> {
                billItemListSignal.clear();
                productEntityIntegerMap.forEach((productEntity, integer) -> {
                    BillItem billItem = new BillItem(gridBillItemsCounter.getAndIncrement(), productEntity, integer);
                    billItemListSignal.insertLast(billItem);
                });
            };
        }
        return productEntityIntegerMap -> {
            productEntityIntegerMap.forEach((productEntity, integer) -> {
                BillItem billItem = new BillItem(gridBillItemsCounter.getAndIncrement(), productEntity, integer);
                billItemListSignal.insertLast(billItem);
            });
        };
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
        billFormStyles(formLayout);

        HorizontalLayout deadLineFieldLayout = new HorizontalLayout();
        deadLineFieldLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        Checkbox boolDeadlineCheckbox = new Checkbox(
                "Deadline Given",
                false
        );
        boolDeadlineCheckbox.bindValue(orderDeadlineSignal, orderDeadlineSignal::set);
        BillDurationField deadlineDurationCountdownField = new BillDurationField();
        deadlineDurationCountdownField.bindEnabled(orderDeadlineSignal);
        deadlineDurationCountdownField.bindVisible(orderDeadlineSignal);
        deadlineDurationCountdownField.bindValue(deadlineDurationSignal, deadlineDurationSignal::set);
        DateTimePicker deadlinePicker = new DateTimePicker("Deadline");
        deadlinePicker.setReadOnly(true);
        deadlinePicker.bindVisible(orderDeadlineSignal);
        deadlinePicker.bindValue(orderDueDateTime, null);

        deadLineFieldLayout.add(
                boolDeadlineCheckbox,
                deadlineDurationCountdownField,
                deadlinePicker
        );

        RadioButtonGroup<OrderType> billTypeGroup = new RadioButtonGroup<>();
        billTypeGroup.setItems(OrderType.values());
        billTypeGroup.setValue(OrderType.TRAIN);
        billTypeGroup.bindValue(orderTypeValueSignal, orderTypeValueSignal::set);
        billTypeGroup.setItemLabelGenerator(Enum::name);

        settingBinder(
                billTypeGroup,
                boolDeadlineCheckbox,
                deadlinePicker
        );

        formLayout.addFormItem(
                billTypeGroup,
                "Bill Type"
        );
        formLayout.addFormItem(
                deadLineFieldLayout,
                "Duration To Deadline"
        );
        return formLayout;
    }

    private static void billFormStyles(FormLayout formLayout) {
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep(
                "0",
                1
        ));
        formLayout.addClassNames(
                "bill-form",
                "field-form"
        );
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
                .bind(
                        OrderEntity::isBearDeadline,
                        OrderEntity::setBearDeadline
                );
        this.binder.forField(deadlinePicker)
                .withValidator(new DateTimeRangeValidator(
                        "not pasted datetime",
                        LocalDateTime.now(),
                        LocalDateTime.MAX
                ))
                .bind(
                        OrderEntity::getDeadLine,
                        OrderEntity::setDeadLine
                );
    }

    private void renewBinderAndObject() {
        if (this.editModeOrderEntity != null) {
            this.binder.readBean(this.orderEntity = this.editModeOrderEntity);
            return;
        }
        this.binder.readBean(this.orderEntity = new OrderEntity());
    }

    private Grid<BillItem> assembleBillItemGrid() {
        Grid<BillItem> grid = new Grid<>(BillItem.class, false);
        grid.setWidthFull();
        grid.addThemeVariants(
                GridVariant.LUMO_NO_ROW_BORDERS,
                GridVariant.LUMO_NO_ROW_BORDERS
        );
        grid.addColumn(BillItem::serial)
                .setHeader("#");
        grid.addColumn(buildItemCard())
                .setHeader("Item");
        grid.addComponentColumn(buildItemAmountField())
                .setHeader("Amount Operation");
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        Signal.effect(
                grid,
                () -> {
                    List<BillItem> billItems = this.billItemListSignal.getValues().toList();
                    grid.setItems(billItems);
                }
        );

        return grid;
    }

    private ComponentRenderer<Div, BillItem> buildItemCard() {
        return new ComponentRenderer<>(billItem -> {
            ProductEntity productEntity = billItem.productEntity();
            WikiCrawledEntity crawledAsImage = productEntity.getCrawledAsImage();

            Div card = new Div();
            card.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.START,
                    LumoUtility.Padding.XSMALL,
                    LumoUtility.Margin.XSMALL
            );

            Image image = ProductImages.productImage(
                    productEntity.getName(),
                    crawledAsImage
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
        description.add(
                item,
                factory
        );
        return description;
    }

    private ValueProvider<BillItem, IntegerField> buildItemAmountField() {
        return (item) -> {
            IntegerField integerField = new IntegerField();
            integerField.setValue(item.amount());
            integerField.setStep(1);
            integerField.setStepButtonsVisible(true);
            integerField.setMin(1);
            integerField.addValueChangeListener(
                    fieldChanged -> {
                        Integer amount = fieldChanged.getValue();
                        Optional<ValueSignal<BillItem>> existingItem = billItemListSignal.peek()
                                .stream()
                                .filter(signal -> signal.peek()
                                        .productEntity()
                                        .equals(item.productEntity()))
                                .findFirst();
                        existingItem.ifPresent(
                                existing -> existing.update(
                                        itemInSignal -> itemInSignal.update(amount)
                                )
                        );
                    }
            );
            return integerField;
        };
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
            button.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_LARGE
            );
            button.addClickListener(_ -> {
                this.productsAmountPanel.consume();
                dialog.close();
            });
            dialog.getFooter().add(button);
            dialog.open();
        });

        return addItemButton;
    }

    private Component assembleFooterPanel(AtomicReference<Dialog> dialogReference) {
        HorizontalLayout footerLayout = new HorizontalLayout();

        Button submit = new Button("Submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(_ -> {
            onSubmit();
            dialogReference.get().close();
        });

        Button cancel = new Button("Cancel");
        cancel.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR
        );
        cancel.addClickListener(_ -> dialogReference.get().close());

        footerLayout.add(
                submit,
                cancel
        );
        footerLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        footerLayout.setAlignItems(Alignment.BASELINE);
        return footerLayout;
    }

    private void onSubmit() {
        try {
            this.orderEntity.setCreatedDateTime(orderReleaseDateTime.peek());
            if (!this.orderEntity.isBearDeadline()) {
                this.orderEntity.setDeadLine(null);
            }

            getBillItemListSignal().peekValues().forEach(
                    billItem -> this.orderEntity.itemAdd(
                            billItem.productEntity(),
                            billItem.amount()
                    )
            );

            Optional.ofNullable(
                            getOrderListViewPresenter().getTownshipAuthenticationContext()
                    )
                    .map(TownshipAuthenticationContext::getUserDetails)
                    .map(AccountEntity::getPlayerEntity)
                    .ifPresentOrElse(
                            player -> {
                                orderEntity.setPlayerEntity(player);
                            }, () -> {
                                throw new RuntimeException("no player entity,meaningless order");
                            }
                    );

            getBinder().writeBean(this.orderEntity);
            getOrderListViewPresenter().getOrderEntityRepository().saveAndFlush(this.orderEntity);
            getOrderListViewPresenter().updateOrderListSignal();
        } catch (ValidationException e) {
            Notification.show(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public OrderFormView(
            OrderListView orderListView,
            OrderListViewPresenter orderListViewPresenter,
            AtomicReference<Dialog> dialogReference
    ) {
        this.orderListView = orderListView;
        this.orderListViewPresenter = orderListViewPresenter;
        this.productsAmountPanel = new ProductsAmountPanel(
                this.orderListViewPresenter.getCollectionSupplier(),
                billItemListSignalConsumer()
        );
        this.editMode = false;

        style();
        add(assembleBillForm());
        addAndExpand(this.billItemGrid = assembleBillItemGrid());
        add(assembleItemAppendBtn());
        add(assembleFooterPanel(dialogReference));

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        TaskScheduler taskScheduler = this.getOrderListViewPresenter().getTaskScheduler();
        scheduledFuture = taskScheduler.scheduleAtFixedRate(
                () -> {
                    orderReleaseDateTime.set(LocalDateTime.now());
                }, Duration.ofSeconds(1)
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        scheduledFuture.cancel(true);
    }

}
