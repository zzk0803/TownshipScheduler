package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.ui.components.FieldFactoryDetailsCustomField;
import zzk.townshipscheduler.ui.eventbus.UiEventBus;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class PlayerFieldFactoryArticleForm extends Composite<VerticalLayout> {

    private final PlayerService playerService;

    private final PlayerEntity playerEntity;

    private Set<FieldFactoryEntity> inGridFieldFactorySet;

    private ListDataProvider<FieldFactoryEntity> inGridFieldFactoryDataProvider;

    private ComboBox<FieldFactoryInfoEntity> fieldFactoryInfoComboBox;

    private Grid<FieldFactoryEntity> factoryEntityGrid;

    private Set<Binder<FieldFactoryEntity>> fieldFactoryBinderSet = new LinkedHashSet<>();

    public PlayerFieldFactoryArticleForm(
            PlayerEntity playerEntity,
            PlayerService playerService
    ) {
        this.playerEntity = playerEntity;
        this.playerService = playerService;

        inGridFieldFactorySet = new LinkedHashSet<>();
        inGridFieldFactoryDataProvider = new ListDataProvider<>(inGridFieldFactorySet);

        buildFieldFactorySelectingComboBox();

        buildFieldFactoryEditGrid();

        UiEventBus.subscribe(
                this,
                PlayerFieldFactoryPersistRequestEvent.class,
                componentEvent -> {
                    fieldFactoryBinderSet.stream()
                            .map(Binder::getBean)
                            .forEach(fieldFactoryEntity -> {
                                try {
                                    playerService.updatePlayerWithFieldFactory(playerEntity, fieldFactoryEntity);
                                } catch (Exception e) {
                                    Notification notification = Notification.show(
                                            "error when update",
                                            3,
                                            Notification.Position.BOTTOM_END
                                    );
                                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                    notification.open();
                                }
                            });
                }
        );

        getContent().add(fieldFactoryInfoComboBox, factoryEntityGrid);
    }

    private void buildFieldFactoryEditGrid() {
        final Grid<FieldFactoryEntity> factoryEntityGrid;
        factoryEntityGrid = new Grid<>(FieldFactoryEntity.class, false);

        Grid.Column<FieldFactoryEntity> getFieldFactoryInfoEntityColumn
                = factoryEntityGrid.addColumn(FieldFactoryEntity::getFieldFactoryInfoEntity)
                .setHeader("Field&Factory Type");
        Grid.Column<FieldFactoryEntity> instanceDetailsColumn
                = factoryEntityGrid.addColumn(new ComponentRenderer<>(fieldFactoryEntity -> {
            Map<Integer, FieldFactoryEntity.FieldFactoryDetails> instanceSequenceDetailsMap = fieldFactoryEntity.getInstanceSequenceDetailsMap();
            VerticalLayout renderedContent = new VerticalLayout();
            renderedContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            renderedContent.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
            instanceSequenceDetailsMap.entrySet().stream()
                    .map(integerFieldFactoryDetailsEmbedEntry -> {
                        Integer sequence = integerFieldFactoryDetailsEmbedEntry.getKey();
                        FieldFactoryEntity.FieldFactoryDetails factoryDetailsEmbed = integerFieldFactoryDetailsEmbedEntry.getValue();
                        HorizontalLayout horizontalLayout = new HorizontalLayout();
                        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
                        horizontalLayout.add(
                                new Text("#" + sequence),
                                new Text(" Producing Length=" + factoryDetailsEmbed.getProducingLength()),
                                new Text(" Reap Window Size=" + factoryDetailsEmbed.getReapWindowSize())
                        );
                        return horizontalLayout;
                    })
                    .forEach(renderedContent::add);
            return renderedContent;
        })).setHeader("Instance Details").setFlexGrow(1);
        Grid.Column<FieldFactoryEntity> availableOperationColumn = factoryEntityGrid.addColumn(
                new ComponentRenderer<>(fieldFactoryEntity -> new Text("N/A"))
        ).setHeader("Available Operation");

        factoryEntityGrid.setEmptyStateText("Nothing Here...For Now");
        factoryEntityGrid.setSelectionMode(Grid.SelectionMode.NONE);
        factoryEntityGrid.setAllRowsVisible(true);
        factoryEntityGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        factoryEntityGrid.setItems(inGridFieldFactoryDataProvider);

        UiEventBus.subscribe(
                this,
                PlayerFieldFactoryArticleFormFactoryInfoChooseEvent.class,
                componentEvent -> {
                    buildFieldFactoryInGridEditor(
                            componentEvent,
                            factoryEntityGrid,
                            instanceDetailsColumn,
                            availableOperationColumn
                    );
                }
        );

        this.factoryEntityGrid = factoryEntityGrid;
    }

    private void buildFieldFactoryInGridEditor(
            PlayerFieldFactoryArticleFormFactoryInfoChooseEvent componentEvent,
            Grid<FieldFactoryEntity> factoryEntityGrid,
            Grid.Column<FieldFactoryEntity> instanceDetailsColumn,
            Grid.Column<FieldFactoryEntity> availableOperationColumn
    ) {
        Editor<FieldFactoryEntity> gridEditor = factoryEntityGrid.getEditor();
        Binder<FieldFactoryEntity> binder = new Binder<>(FieldFactoryEntity.class);

        FieldFactoryInfoEntity fieldFactoryInfoEntity = componentEvent.getFieldFactoryInfoEntity();
        FieldFactoryEntity fieldFactoryEntity = fieldFactoryInfoEntity.toFieldFactoryEntity();

        inGridFieldFactorySet.add(fieldFactoryEntity);
        inGridFieldFactoryDataProvider.refreshAll();
        gridEditor.editItem(fieldFactoryEntity);

        gridEditor.setBinder(binder);
        binder.setBean(fieldFactoryEntity);
        fieldFactoryBinderSet.add(binder);

        AtomicInteger seqRoller = new AtomicInteger(1);
        instanceDetailsColumn.setEditorComponent(
                doBuildGridEditorComponent(
                        seqRoller,
                        fieldFactoryEntity,
                        binder
                )
        );

        Button cancelButton = new Button(VaadinIcon.CLOSE.create());
        cancelButton.addThemeVariants(
                ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_TERTIARY
        );
        cancelButton.addClickListener(_ -> {
            fieldFactoryBinderSet.remove(binder);
            inGridFieldFactorySet.remove(fieldFactoryEntity);
            inGridFieldFactoryDataProvider.refreshAll();
            gridEditor.cancel();
        });
        availableOperationColumn.setEditorComponent(cancelButton);

    }

    private VerticalLayout doBuildGridEditorComponent(
            AtomicInteger seqRoller,
            FieldFactoryEntity fieldFactory,
            Binder<FieldFactoryEntity> binder
    ) {
        VerticalLayout outerWrapper = new VerticalLayout();
        outerWrapper.setWidthFull();
        outerWrapper.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        VerticalLayout formWrapper = new VerticalLayout();
        formWrapper.setWidthFull();
        formWrapper.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        FieldFactoryDetailsCustomField fieldFactoryDetailsRow
                = doBuildFieldFactoryInGridEditorCreateDetailsFormRow(
                seqRoller,
                binder,
                fieldFactory
        );
        formWrapper.add(fieldFactoryDetailsRow);
        outerWrapper.addAndExpand(formWrapper);

        Button addButton = new Button(
                VaadinIcon.PLUS.create(),
                click -> {
                    formWrapper.add(
                            doBuildFieldFactoryInGridEditorCreateDetailsFormRow(
                                    seqRoller,
                                    binder,
                                    fieldFactory
                            )
                    );
                }
        );
        HorizontalLayout addButtonWrapper = new HorizontalLayout(addButton);
        addButtonWrapper.setWidthFull();
        addButtonWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        outerWrapper.add(addButtonWrapper);
        return outerWrapper;
    }

    private FieldFactoryDetailsCustomField doBuildFieldFactoryInGridEditorCreateDetailsFormRow(
            AtomicInteger seqRoller,
            Binder<FieldFactoryEntity> binder,
            FieldFactoryEntity fieldFactoryEntity
    ) {
        int i = seqRoller.getAndIncrement();
        FieldFactoryInfoEntity fieldFactoryInfoEntity = fieldFactoryEntity.getFieldFactoryInfoEntity();
        FieldFactoryEntity.FieldFactoryDetails fieldFactoryDetailsField = fieldFactoryEntity.appendFieldFactoryDetails();
        FieldFactoryDetailsCustomField customField = new FieldFactoryDetailsCustomField(fieldFactoryDetailsField);
        binder.forField(customField)
                .withValidator((fieldFactoryDetails, valueContext) -> {
                    int producingLength = fieldFactoryDetails.getProducingLength();
                    int reapWindowSize = fieldFactoryDetails.getReapWindowSize();

                    boolean producingLengthOutRange
                            = producingLength < fieldFactoryInfoEntity.getDefaultProducingCapacity()
                              || producingLength > fieldFactoryInfoEntity.getMaxProducingCapacity();
                    if (producingLengthOutRange) {
                        return ValidationResult.error(
                                "producing length should between %s and %s".formatted(
                                        fieldFactoryInfoEntity.getDefaultProducingCapacity(),
                                        fieldFactoryInfoEntity.getMaxProducingCapacity()
                                )
                        );
                    }

                    boolean reapWindowSizeOutRange
                            = reapWindowSize < fieldFactoryInfoEntity.getDefaultReapWindowCapacity()
                              || reapWindowSize > fieldFactoryInfoEntity.getMaxReapWindowCapacity();
                    if (reapWindowSizeOutRange) {
                        return ValidationResult.error(
                                "producing length should between %s and %s".formatted(
                                        fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                                        fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                                )
                        );
                    }
                    return ValidationResult.ok();
                })
                .bind(
                        binderFieldFactory -> binderFieldFactory.getInstanceSequenceDetailsMap()
                                .get(i)
                        ,
                        (binderFieldFactory, fieldFactoryDetails) ->
                                binderFieldFactory.getInstanceSequenceDetailsMap()
                                        .put(i, fieldFactoryDetails)
                );

        return customField;
    }

    private void buildFieldFactorySelectingComboBox() {
        ComboBox<FieldFactoryInfoEntity> fieldFactoryInfoEntityComboBox = new ComboBox<>();
        fieldFactoryInfoEntityComboBox.setAllowCustomValue(false);
        fieldFactoryInfoEntityComboBox.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        fieldFactoryInfoEntityComboBox.addValueChangeListener(valueChangeEvent -> {
            FieldFactoryInfoEntity fieldFactoryInfo = valueChangeEvent.getValue();

            UiEventBus.publish(
                    new PlayerFieldFactoryArticleFormFactoryInfoChooseEvent(
                            this,
                            false,
                            fieldFactoryInfo
                    )
            );

        });
        this.fieldFactoryInfoComboBox = fieldFactoryInfoEntityComboBox;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.fieldFactoryInfoComboBox.setItems(playerService.findAvailableFieldFactoryInfoByPlayer(playerEntity));
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        return verticalLayout;
    }


}
