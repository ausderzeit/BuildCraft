package buildcraft.transport;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.transport.client.PipeRegistryClient;
import buildcraft.transport.client.model.GateMeshDefinition;
import buildcraft.transport.client.model.key.*;
import buildcraft.transport.client.model.plug.PlugBakerLens;
import buildcraft.transport.client.model.plug.PlugGateBaker;
import buildcraft.transport.client.render.*;
import buildcraft.transport.container.*;
import buildcraft.transport.gui.*;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.plug.PluggableGate;
import buildcraft.transport.plug.PluggablePulsar;
import buildcraft.transport.tile.TileFilteredBuffer;
import buildcraft.transport.tile.TilePipeHolder;

public abstract class BCTransportProxy implements IGuiHandler {
    @SidedProxy(modId = BCTransport.MODID)
    private static BCTransportProxy proxy;

    public static BCTransportProxy getProxy() {
        return proxy;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BCTransportGuis gui = BCTransportGuis.get(id);
        if (gui == null) return null;
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        switch (gui) {
            case FILTERED_BUFFER: {
                if (tile instanceof TileFilteredBuffer) {
                    TileFilteredBuffer filteredBuffer = (TileFilteredBuffer) tile;
                    return new ContainerFilteredBuffer_BC8(player, filteredBuffer);
                }
                break;
            }
            case PIPE_DIAMOND: {
                if (tile instanceof IPipeHolder) {
                    IPipeHolder holder = (IPipeHolder) tile;
                    IPipe pipe = holder.getPipe();
                    if (pipe == null) return null;
                    PipeBehaviour behaviour = pipe.getBehaviour();
                    if (behaviour instanceof PipeBehaviourDiamond) {
                        PipeBehaviourDiamond diaPipe = (PipeBehaviourDiamond) behaviour;
                        return new ContainerDiamondPipe(player, diaPipe);
                    }
                }
                break;
            }
            case PIPE_DIAMOND_WOOD: {
                if (tile instanceof IPipeHolder) {
                    IPipeHolder holder = (IPipeHolder) tile;
                    IPipe pipe = holder.getPipe();
                    if (pipe == null) return null;
                    PipeBehaviour behaviour = pipe.getBehaviour();
                    if (behaviour instanceof PipeBehaviourWoodDiamond) {
                        PipeBehaviourWoodDiamond diaPipe = (PipeBehaviourWoodDiamond) behaviour;
                        return new ContainerDiamondWoodPipe(player, diaPipe);
                    }
                }
                break;
            }
            case PIPE_EMZULI: {
                if (tile instanceof IPipeHolder) {
                    IPipeHolder holder = (IPipeHolder) tile;
                    IPipe pipe = holder.getPipe();
                    if (pipe == null) return null;
                    PipeBehaviour behaviour = pipe.getBehaviour();
                    if (behaviour instanceof PipeBehaviourEmzuli) {
                        PipeBehaviourEmzuli emPipe = (PipeBehaviourEmzuli) behaviour;
                        return new ContainerEmzuliPipe_BC8(player, emPipe);
                    }
                }
                break;
            }
            case GATE: {
                int ry = y >> 3;
                EnumFacing gateSide = EnumFacing.getFront(y & 0x7);
                tile = world.getTileEntity(new BlockPos(x, ry, z));
                if (tile instanceof IPipeHolder) {
                    IPipeHolder holder = (IPipeHolder) tile;
                    PipePluggable plug = holder.getPluggable(gateSide);
                    if (plug instanceof PluggableGate) {
                        return new ContainerGate(player, ((PluggableGate) plug).logic);
                    }
                }
                break;
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public void fmlPreInit() {}

    public void fmlInit() {}

    public void fmlPostInit() {}

    @SideOnly(Side.SERVER)
    public static class ServerProxy extends BCTransportProxy {

    }

    @SideOnly(Side.CLIENT)
    public static class ClientProxy extends BCTransportProxy {
        @Override
        public void fmlPreInit() {
            BCTransportSprites.fmlPreInit();
            BCTransportModels.fmlPreInit();
            PipeApiClient.registry = PipeRegistryClient.INSTANCE;
        }

        @Override
        public void fmlInit() {
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(BCTransportItems.plugGate, GateMeshDefinition.INSTANCE);
            ClientRegistry.bindTileEntitySpecialRenderer(TilePipeHolder.class, new RenderPipeHolder());

            PipeApiClient.registry.registerBaker(KeyPlugGate.class, PlugGateBaker.INSTANCE);
            PipeApiClient.registry.registerBaker(KeyPlugBlocker.class, BCTransportModels.BAKER_PLUG_BLOCKER);
            PipeApiClient.registry.registerBaker(KeyPlugPulsar.class, BCTransportModels.BAKER_PLUG_PULSAR);
            PipeApiClient.registry.registerBaker(KeyPlugLightSensor.class, BCTransportModels.BAKER_PLUG_LIGHT_SENSOR);
            PipeApiClient.registry.registerBaker(KeyPlugLens.class, PlugBakerLens.INSTANCE);

            PipeApiClient.registry.registerRenderer(PluggableGate.class, PlugGateRenderer.INSTANCE);
            PipeApiClient.registry.registerRenderer(PluggablePulsar.class, PlugPulsarRenderer.INSTANCE);

            PipeApiClient.registry.registerRenderer(PipeFlowItems.class, PipeFlowRendererItems.INSTANCE);
            PipeApiClient.registry.registerRenderer(PipeFlowFluids.class, PipeFlowRendererFluids.INSTANCE);
            PipeApiClient.registry.registerRenderer(PipeFlowPower.class, PipeFlowRendererPower.INSTANCE);

            PipeApiClient.registry.registerRenderer(PipeBehaviourStripes.class, PipeBehaviourRendererStripes.INSTANCE);
        }

        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            BCTransportGuis gui = BCTransportGuis.get(id);
            if (gui == null) {
                return null;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            switch (gui) {
                case FILTERED_BUFFER: {
                    if (tile instanceof TileFilteredBuffer) {
                        TileFilteredBuffer filteredBuffer = (TileFilteredBuffer) tile;
                        return new GuiFilteredBuffer(new ContainerFilteredBuffer_BC8(player, filteredBuffer));
                    }
                    break;
                }
                case PIPE_DIAMOND: {
                    if (tile instanceof IPipeHolder) {
                        IPipeHolder holder = (IPipeHolder) tile;
                        IPipe pipe = holder.getPipe();
                        if (pipe == null) return null;
                        PipeBehaviour behaviour = pipe.getBehaviour();
                        if (behaviour instanceof PipeBehaviourDiamond) {
                            PipeBehaviourDiamond diaPipe = (PipeBehaviourDiamond) behaviour;
                            return new GuiDiamondPipe(player, diaPipe);
                        }
                    }
                    break;
                }
                case PIPE_DIAMOND_WOOD: {
                    if (tile instanceof IPipeHolder) {
                        IPipeHolder holder = (IPipeHolder) tile;
                        IPipe pipe = holder.getPipe();
                        if (pipe == null) return null;
                        PipeBehaviour behaviour = pipe.getBehaviour();
                        if (behaviour instanceof PipeBehaviourWoodDiamond) {
                            PipeBehaviourWoodDiamond diaPipe = (PipeBehaviourWoodDiamond) behaviour;
                            return new GuiDiamondWoodPipe(player, diaPipe);
                        }
                    }
                    break;
                }
                case PIPE_EMZULI: {
                    if (tile instanceof IPipeHolder) {
                        IPipeHolder holder = (IPipeHolder) tile;
                        IPipe pipe = holder.getPipe();
                        if (pipe == null) return null;
                        PipeBehaviour behaviour = pipe.getBehaviour();
                        if (behaviour instanceof PipeBehaviourEmzuli) {
                            PipeBehaviourEmzuli emzPipe = (PipeBehaviourEmzuli) behaviour;
                            return new GuiEmzuliPipe_BC8(player, emzPipe);
                        }
                    }
                    break;
                }
                case GATE: {
                    int ry = y >> 3;
                    EnumFacing gateSide = EnumFacing.getFront(y & 0x7);
                    tile = world.getTileEntity(new BlockPos(x, ry, z));
                    if (tile instanceof IPipeHolder) {
                        IPipeHolder holder = (IPipeHolder) tile;
                        PipePluggable plug = holder.getPluggable(gateSide);
                        if (plug instanceof PluggableGate) {
                            return new GuiGate(new ContainerGate(player, ((PluggableGate) plug).logic));
                        }
                    }
                    break;
                }
            }
            return null;
        }
    }
}
