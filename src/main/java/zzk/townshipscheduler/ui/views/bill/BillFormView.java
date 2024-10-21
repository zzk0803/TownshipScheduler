package zzk.townshipscheduler.ui.views.bill;

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
import com.vaadin.flow.theme.lumo.LumoUtility;
import zzk.townshipscheduler.backend.persistence.Bill;
import zzk.townshipscheduler.backend.persistence.BillType;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.ui.BillDurationField;
import zzk.townshipscheduler.ui.GoodsCategoriesPanel;
import zzk.townshipscheduler.adopting.form.BillItem;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;

@Route(value = "bills/form", registerAtStartup = false)
public class BillFormView
        extends VerticalLayout {

    private final BillFormPresenter presenter;

    private final GoodsCategoriesPanel goodsCategoriesPanel;

    private Grid<BillItem> billItemGrid;

    public BillFormView(
            BillFormPresenter billFormPresenter,
            GoodsCategoriesPanel goodsCategoriesPanel
    ) {
        this.presenter = billFormPresenter;
        this.goodsCategoriesPanel = goodsCategoriesPanel;
        this.presenter.setBillFormView(this);
        setupView();

        add(assembleBillForm());

        add(assembleBillItemGrid());

        add(assembleItemAppendBtn());

        add(assembleFooterPanel());
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

    private void setupView() {
        addClassName("bill-form");
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

        boolDeadlineCheckbox.addValueChangeListener(valueChange -> {
            settingDeadlineFieldGroupAvailableStatus(valueChange.getValue(), durationCountdownField, deadlinePicker);
        });

        deadLineFieldLayout.add(boolDeadlineCheckbox, durationCountdownField, deadlinePicker);

        RadioButtonGroup<BillType> billTypeGroup = new RadioButtonGroup<>();
        billTypeGroup.setItems(BillType.values());
        billTypeGroup.setValue(BillType.HELICOPTER);

        settingBinder(billTypeGroup, boolDeadlineCheckbox, deadlinePicker, durationCountdownField);

        formLayout.addFormItem(billTypeGroup, "Bill Type");
        formLayout.addFormItem(deadLineFieldLayout, "Duration To Deadline");
        return formLayout;
    }

    private Grid<BillItem> assembleBillItemGrid() {
        Grid<BillItem> grid = new Grid<>(BillItem.class, false);
        grid.addThemeVariants(
                GridVariant.LUMO_NO_ROW_BORDERS,
                GridVariant.LUMO_NO_ROW_BORDERS
        );

        grid.addColumn(BillItem::getSerial)
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(buildItemCard())
                .setAutoWidth(false)
                .setFlexGrow(1);
        grid.addComponentColumn(buildItemAmountField())
                .setAutoWidth(true)
                .setFlexGrow(0);
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
        addItemButton.addClickListener(click -> {
            Dialog dialog = new Dialog("Select Goods...");
            dialog.setSizeFull();
            dialog.addComponentAsFirst(goodsCategoriesPanel);

            Button button = new Button("OK");
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            button.addClickListener(dialogCloseClick -> {
                goodsCategoriesPanel.consumeSelected(goods -> {
                    BillItem billItem = new BillItem(
                            presenter.getGridBillItemsCounter().incrementAndGet(),
                            goods,
                            1
                    );
                    presenter.addBillItem(billItem);
                });
                dialog.close();
                presenter.setupDataProviderForItems(billItemGrid);
            });
            dialog.getFooter().add(button);
            dialog.open();
        });

        return addItemButton;
    }

    private Component assembleFooterPanel() {
        HorizontalLayout footer = buildFooterComponent();

        Button submit = new Button("Submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(submitClick -> {
            presenter.onSubmit();
            UI.getCurrent().navigate(BillListView.class);
        });

        Button cancel = new Button("Cancel");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        cancel.addClickListener(cancelClick -> {
            UI.getCurrent().navigate(BillListView.class);
        });

        footer.add(submit, cancel);
        footer.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        footer.setAlignItems(Alignment.BASELINE);
        return footer;
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
            RadioButtonGroup<BillType> billTypeGroup,
            Checkbox boolDeadlineCheckbox,
            DateTimePicker deadlinePicker,
            BillDurationField durationCountdownField
    ) {
        Binder<Bill> binder = presenter.prepareBillAndBinder();
        binder.forField(billTypeGroup)
                .asRequired()
                .bind(Bill::getBillType, Bill::setBillType);
        binder.forField(boolDeadlineCheckbox)
                .bind(Bill::isBoolDeadLine, Bill::setBoolDeadLine);
        binder.forField(deadlinePicker)
                .withValidator(new DateTimeRangeValidator(
                        "not pasted datetime",
                        LocalDateTime.now(),
                        LocalDateTime.MAX
                ))
                .bind(
                        Bill::getDeadLine,
                        Bill::setDeadLine
                );
        binder.forField(durationCountdownField)
                .withConverter(getDurationLocalDateTimeConverter())
                .bind(
                        Bill::getDeadLine,
                        Bill::setDeadLine
                );
    }

    private ComponentRenderer<Div, BillItem> buildItemCard() {
        return new ComponentRenderer<>(billItem -> {
            Goods goods = billItem.getGoods();

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
                    goods.getName(),
                    () -> new ByteArrayInputStream(goods.getImageBytes())
            ));
            card.add(image);

            Div description = new Div();
            description.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.BASELINE,
                    LumoUtility.JustifyContent.END,
                    LumoUtility.TextColor.SECONDARY
            );
            Span item = new Span("Item:" + goods.getName());
            item.addClassNames(LumoUtility.Display.FLEX);
            Span factory = new Span("Factory:" + goods.getCategory());
            factory.addClassNames(LumoUtility.Display.FLEX);
            description.add(item, factory);
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
            integerField.setMin(0);
            integerField.addValueChangeListener(fieldChanged -> {
                item.setAmount(fieldChanged.getValue());
                billItemGrid.getDataProvider().refreshItem(item);
            });
            return integerField;
        };
    }

    private HorizontalLayout buildFooterComponent() {
        return new HorizontalLayout();
    }

    private Converter<Duration, LocalDateTime> getDurationLocalDateTimeConverter() {
        return new Converter<Duration, LocalDateTime>() {
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
