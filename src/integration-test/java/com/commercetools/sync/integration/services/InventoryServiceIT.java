package com.commercetools.sync.integration.services;

import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.impl.InventoryServiceImpl;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.updateactions.ChangeQuantity;
import io.sphere.sdk.inventory.commands.updateactions.SetExpectedDelivery;
import io.sphere.sdk.inventory.commands.updateactions.SetRestockableInDays;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryServiceIT {

    private InventoryService inventoryService;

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
        populateTargetProject();
        inventoryService = new InventoryServiceImpl(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
    }

    @Test
    public void fetchInventoryEntriesBySku_ShouldReturnCorrectInventoryEntriesWithoutReferenceExpansion() {
        final Set<String> skus = singleton(SKU_1);
        final List<InventoryEntry> result = inventoryService.fetchInventoryEntriesBySkus(skus)
                                                            .toCompletableFuture()
                                                            .join();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        //assert SKU is correct
        result.stream()
              .map(InventoryEntry::getSku)
              .forEach(sku -> assertThat(sku).isEqualTo(SKU_1));

        //assert references are not expanded
        result.stream()
              .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
              .map(InventoryEntry::getSupplyChannel)
              .forEach(supplyChannel -> assertThat(supplyChannel.getObj()).isNull());
    }

    @Test
    public void createInventoryEntry_ShouldCreateCorrectInventoryEntry() {
        //prepare draft and create
        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();

        final InventoryEntry result = inventoryService.createInventoryEntry(inventoryEntryDraft)
                                                      .toCompletableFuture()
                                                      .join();
        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(result.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
        assertThat(result.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);

        //assert CTP state
        final Optional<InventoryEntry> updatedInventoryEntry =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
        assertThat(updatedInventoryEntry).isNotEmpty();
        assertThat(updatedInventoryEntry.get()).isEqualTo(result);
    }

    @Test
    public void updateInventoryEntry_ShouldUpdateInventoryEntry() {
        //fetch existing inventory entry and assert its state
        final Optional<InventoryEntry> existingEntryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(existingEntryOptional).isNotEmpty();

        final InventoryEntry existingEntry = existingEntryOptional.get();
        assertThat(existingEntry.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);
        assertThat(existingEntry.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);
        assertThat(existingEntry.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);

        //build update actions and do update
        final List<UpdateAction<InventoryEntry>> updateActions = Stream.of(
            ChangeQuantity.of(QUANTITY_ON_STOCK_2),
            SetExpectedDelivery.of(EXPECTED_DELIVERY_2),
            SetRestockableInDays.of(RESTOCKABLE_IN_DAYS_2)
        ).collect(toList());

        final InventoryEntry result = inventoryService.updateInventoryEntry(existingEntry, updateActions)
                                                      .toCompletableFuture()
                                                      .join();
        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(result.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
        assertThat(result.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);

        //assert CTP state
        final Optional<InventoryEntry> updatedInventoryEntry =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(updatedInventoryEntry).isNotEmpty();
        assertThat(updatedInventoryEntry.get()).isEqualTo(result);
    }
}
