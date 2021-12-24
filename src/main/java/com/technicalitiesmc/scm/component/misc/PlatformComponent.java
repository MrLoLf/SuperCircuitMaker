package com.technicalitiesmc.scm.component.misc;

import com.technicalitiesmc.lib.circuit.component.*;
import com.technicalitiesmc.lib.circuit.interfaces.RedstoneSink;
import com.technicalitiesmc.lib.circuit.interfaces.RedstoneSource;
import com.technicalitiesmc.lib.circuit.interfaces.wire.BundledWire;
import com.technicalitiesmc.lib.circuit.interfaces.wire.RedstoneWire;
import com.technicalitiesmc.lib.math.VecDirection;
import com.technicalitiesmc.lib.math.VecDirectionFlags;
import com.technicalitiesmc.scm.component.CircuitComponentBase;
import com.technicalitiesmc.scm.component.InterfaceLookup;
import com.technicalitiesmc.scm.init.SCMComponents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;

import java.util.function.BiFunction;

public class PlatformComponent extends CircuitComponentBase<PlatformComponent> {

    private static final AABB BOUNDS = new AABB(0, 14 / 16D, 0, 1, 1, 1);

    private static final InterfaceLookup<PlatformComponent> INTERFACES = InterfaceLookup.<PlatformComponent>builder()
            // Pass through redstone I/O
            .with(RedstoneSource.class, VecDirectionFlags.verticals(), makePassThrough(RedstoneSource.class, false))
            .with(RedstoneSink.class, VecDirectionFlags.verticals(), makePassThrough(RedstoneSink.class, false))
            // As well as regular and bundled wires
            .with(RedstoneWire.class, VecDirectionFlags.verticals(), makePassThrough(RedstoneWire.class, true))
            .with(BundledWire.class, VecDirectionFlags.verticals(), makePassThrough(BundledWire.class, true))
            .build();

    private boolean conductive = true;

    public PlatformComponent(ComponentContext context) {
        super(SCMComponents.PLATFORM, context, INTERFACES);
    }

    private PlatformComponent(ComponentContext context, boolean conductive) {
        this(context);
        this.conductive = conductive;
    }

    @Override
    public CircuitComponent copyRotated(ComponentContext context, Rotation rotation) {
        return new PlatformComponent(context, conductive);
    }

    @Override
    public AABB getBoundingBox() {
        return BOUNDS;
    }

    @Override
    public boolean isTopSolid() {
        return true;
    }

    @Override
    public void receiveEvent(VecDirection side, CircuitEvent event, ComponentEventMap.Builder builder) {
        // Forward events vertically
        if (conductive && side.getAxis() == Direction.Axis.Y) {
            var neighbor = getOppositeNeighbor(side, false);
            // Skip forwarding the event if there is no direct neighbor
            if (neighbor != null) {
                sendEvent(event, true, side.getOpposite());
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("conductive", conductive);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        this.conductive = tag.getBoolean("conductive");
    }

    // Helpers

    private CircuitComponent getOppositeNeighbor(VecDirection side, boolean adjacentOnly) {
        if (side.isPositive()) {
            return getNeighbor(VecDirection.NEG_Y, adjacentOnly);
        } else {
            return getNeighbor(VecDirection.POS_Y, ComponentSlot.DEFAULT);
        }
    }

    private static <T> BiFunction<PlatformComponent, VecDirection, T> makePassThrough(Class<T> type, boolean adjacentOnly) {
        return (comp, side) -> {
            if (!comp.conductive) {
                return null;
            }
            var neighbor = comp.getOppositeNeighbor(side, adjacentOnly);
            return neighbor == null ? null : neighbor.getInterface(side, type);
        };
    }

    public static class Client extends ClientComponent {

        @Override
        public boolean isTopSolid(ComponentState state) {
            return true;
        }

        @Override
        public AABB getBoundingBox(ComponentState state) {
            return BOUNDS;
        }

    }

}
