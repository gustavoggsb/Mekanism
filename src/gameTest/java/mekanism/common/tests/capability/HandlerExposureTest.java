package mekanism.common.tests.capability;

import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tests.helpers.MekGameTestHelper;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.GameTest;

@ForEachTest(groups = "capability")
public class HandlerExposureTest {

    @GameTest
    @EmptyTemplate
    @TestHolder(description = "Validates that Mekanism bins expose their inventory via the standard item ResourceHandler capability "
                              + "and that aborted transactions leave the contents unchanged (no duplication).")
    public static void binExposesItemResourceHandler(final MekGameTestHelper helper) {
        BlockPos relativePos = new BlockPos(0, 1, 0);
        helper.setBlock(relativePos, MekanismBlocks.BASIC_BIN.defaultState());
        helper.getBlockEntity(relativePos, TileEntityBin.class);

        ItemResource stone = ItemResource.of(Items.STONE);

        // Find a side that exposes an insertable item handler
        ResourceHandler<ItemResource> handler = null;
        for (Direction side : Direction.values()) {
            ResourceHandler<ItemResource> candidate = helper.getCapability(Capabilities.ITEM.block(), relativePos, side);
            if (candidate != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    if (candidate.insert(stone, 1, tx) > 0) {
                        handler = candidate;
                        break;
                    }
                }
            }
        }
        if (handler == null) {
            helper.fail("Mekanism bin did not expose an insertable item ResourceHandler on any side");
            return;
        }
        final ResourceHandler<ItemResource> h = handler;

        // Committed insert of 10
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(h.insert(stone, 10, tx), 10L, "inserted amount");
            tx.commit();
        }
        helper.assertValueEqual(totalOf(h, stone), 10L, "bin contents after committed insert");

        // Aborted insert of 5 -> contents must remain 10 (no duplication)
        try (Transaction tx = Transaction.openRoot()) {
            h.insert(stone, 5, tx);
        }
        helper.assertValueEqual(totalOf(h, stone), 10L, "bin contents after aborted insert (must be unchanged)");

        // Committed extract of 4 -> 6 remain
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(h.extract(stone, 4, tx), 4L, "extracted amount");
            tx.commit();
        }
        helper.assertValueEqual(totalOf(h, stone), 6L, "bin contents after committed extract");

        // Aborted extract of 3 -> contents must remain 6 (no item loss)
        try (Transaction tx = Transaction.openRoot()) {
            h.extract(stone, 3, tx);
        }
        helper.assertValueEqual(totalOf(h, stone), 6L, "bin contents after aborted extract (must be unchanged)");

        helper.succeed();
    }

    @GameTest
    @EmptyTemplate
    @TestHolder(description = "Validates that Mekanism fluid tanks expose their fluid via the standard fluid ResourceHandler capability "
                              + "and that aborted transactions leave the contents unchanged.")
    public static void fluidTankExposesFluidResourceHandler(final MekGameTestHelper helper) {
        BlockPos relativePos = new BlockPos(0, 1, 0);
        helper.setBlock(relativePos, MekanismBlocks.BASIC_FLUID_TANK.defaultState());
        helper.getBlockEntity(relativePos, TileEntityFluidTank.class);

        FluidResource water = FluidResource.of(Fluids.WATER);

        ResourceHandler<FluidResource> handler = null;
        for (Direction side : Direction.values()) {
            ResourceHandler<FluidResource> candidate = helper.getCapability(Capabilities.FLUID.block(), relativePos, side);
            if (candidate != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    if (candidate.insert(water, 1_000, tx) > 0) {
                        handler = candidate;
                        break;
                    }
                }
            }
        }
        if (handler == null) {
            helper.fail("Mekanism fluid tank did not expose an insertable fluid ResourceHandler on any side");
            return;
        }
        final ResourceHandler<FluidResource> h = handler;

        // Committed insert of 2000 mB
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(h.insert(water, 2_000, tx), 2_000L, "inserted amount");
            tx.commit();
        }
        helper.assertValueEqual(totalOf(h, water), 2_000L, "tank contents after committed insert");

        // Aborted insert of 1000 mB -> contents must remain 2000 (transaction safety)
        try (Transaction tx = Transaction.openRoot()) {
            h.insert(water, 1_000, tx);
        }
        helper.assertValueEqual(totalOf(h, water), 2_000L, "tank contents after aborted insert (must be unchanged)");

        // Committed extract of 500 mB -> 1500 remain
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(h.extract(water, 500, tx), 500L, "extracted amount");
            tx.commit();
        }
        helper.assertValueEqual(totalOf(h, water), 1_500L, "tank contents after committed extract");

        helper.succeed();
    }

    private static long totalOf(ResourceHandler<ItemResource> handler, ItemResource resource) {
        long total = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource found = handler.getResource(i);
            if (!found.isEmpty() && found.equals(resource)) {
                total += handler.getAmountAsLong(i);
            }
        }
        return total;
    }

    private static long totalOf(ResourceHandler<FluidResource> handler, FluidResource resource) {
        long total = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource found = handler.getResource(i);
            if (!found.isEmpty() && found.equals(resource)) {
                total += handler.getAmountAsLong(i);
            }
        }
        return total;
    }
}
