package com.immortalstorage.immortalstorage.locator;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.*;

/** Baked at world load or datapack reload only; gameplay uses immutable O(1) mappings. */
public final class ToolLocatorLootCatalog {
    private static long generation=-1, revision;
    private static Map<ResourceLocation,Selection> selections=Map.of();
    private static final Selection ALL=new Selection(true,false,List.of());
    public record Selection(boolean ready,boolean filtering,List<ResourceLocation> ores) {}
    private ToolLocatorLootCatalog() {}
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event){rebuild(event.getServer());}
    public static void rebuild(MinecraftServer server) {
        if(generation==ToolLocatorSettings.generation())return;
        var builder=new Builder(server);
        while(!builder.done())builder.advance(256);
        var index=builder.finish();var resolved=new HashMap<ResourceLocation,Selection>();
        index.dropSources().forEach((item,ores)->{
            var id=ResourceLocation.parse(item);
            if(BuiltInRegistries.ITEM.getOptional(id).isPresent())resolved.put(id,selection(ores));
        });
        index.oreVariants().forEach((block,ores)->BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(block)).ifPresent(value->{
            if(value.asItem()!=net.minecraft.world.item.Items.AIR)resolved.put(BuiltInRegistries.ITEM.getKey(value.asItem()),selection(ores));
        }));
        selections=Map.copyOf(resolved);generation=ToolLocatorSettings.generation();revision++;
        ImmortalStorageMod.LOG.info("Built ore loot dictionary: {} ore blocks, {} offhand mappings; revision {}",builder.ores.size(),selections.size(),revision);
    }
    private static Selection selection(Set<String> ores){return new Selection(true,true,ores.stream().sorted().map(ResourceLocation::parse).toList());}
    public static Selection select(ItemStack stack) {
        if(generation<0)return new Selection(false,false,List.of());
        return stack.isEmpty()?ALL:selections.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()),ALL);
    }
    public static long revision(){return revision;}
    public static void clear(){generation=-1;selections=Map.of();revision++;}

    private static final class Builder {
        final MinecraftServer server;
        final Map<String,String> ores=new LinkedHashMap<>();final Set<String> oreItems=new HashSet<>();
        final Map<String,JsonElement> definitions=new HashMap<>();final Set<String> absent=new HashSet<>();
        final ArrayDeque<String> queue=new ArrayDeque<>();
        final Map<String,Set<String>> tags=new HashMap<>();
        Builder(MinecraftServer server) {
            this.server=server;
            for(var block:BuiltInRegistries.BLOCK)if((ToolLocatorRules.stateMask(block.defaultBlockState())&ToolLocatorRules.ORE)!=0) {
                var key=block.getLootTable();
                String table=key==null?"minecraft:empty":key.location().toString();
                ores.put(BuiltInRegistries.BLOCK.getKey(block).toString(),table);
                oreItems.add(BuiltInRegistries.ITEM.getKey(block.asItem()).toString());
            }
            queue.addAll(new LinkedHashSet<>(ores.values()));
        }
        void advance(int budget){while(budget-->0&&!queue.isEmpty())load(queue.removeFirst());}
        boolean done(){return queue.isEmpty();}
        JsonElement load(String id) {
            if(definitions.containsKey(id))return definitions.get(id);
            if(absent.contains(id))return null;
            try {
                var table=server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(id)));
                var data=LootTable.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,server.registryAccess()),table).getOrThrow();
                definitions.put(id,data);discoverReferences(data);return data;
            } catch(RuntimeException error) {
                absent.add(id);ImmortalStorageMod.LOG.warn("Cannot trace ore loot table {}: {}",id,error.getMessage());return null;
            }
        }
        void discoverReferences(JsonElement value) { discoverReferences(value,0); }
        void discoverReferences(JsonElement value,int depth) {
            if(depth>64)return;
            if(value.isJsonArray()){for(var v:value.getAsJsonArray())discoverReferences(v,depth+1);return;}
            if(!value.isJsonObject())return;
            var object=value.getAsJsonObject();
            if(object.has("type")&&object.get("type").isJsonPrimitive()&&object.get("type").getAsString().equals("minecraft:loot_table")) {
                var ref=object.has("value")?object.get("value"):object.get("name");
                if(ref!=null&&ref.isJsonPrimitive()){
                    String id=ref.getAsString();if(!definitions.containsKey(id)&&!absent.contains(id)&&!queue.contains(id))queue.addLast(id);
                }
            }
            for(var entry:object.entrySet())if(!entry.getKey().equals("conditions"))discoverReferences(entry.getValue(),depth+1);
        }
        ToolLocatorLootIndex finish() {
            return new ToolLocatorLootIndex(ores,oreItems,definitions::get,id->tags.computeIfAbsent(id,k->{
                var key=TagKey.create(Registries.ITEM,ResourceLocation.parse(k));var items=new HashSet<String>();
                for(var item:BuiltInRegistries.ITEM)if(item.builtInRegistryHolder().is(key))items.add(BuiltInRegistries.ITEM.getKey(item).toString());
                return Set.copyOf(items);
            }));
        }
    }
}
