package com.immortalstorage.immortalstorage.locator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.Function;

/** Data-only reverse loot graph. Names/tags never infer a mineral family. */
public final class ToolLocatorLootIndex {
    private final Map<String,Set<String>> byDrop = new HashMap<>(), byOre = new HashMap<>();
    public ToolLocatorLootIndex(Map<String,String> oreTables, Function<String,JsonElement> tables,
                               Function<String,Set<String>> itemTags) {
        this(oreTables,oreTables.keySet(),tables,itemTags);
    }
    public ToolLocatorLootIndex(Map<String,String> oreTables, Set<String> oreItems, Function<String,JsonElement> tables,
                               Function<String,Set<String>> itemTags) {
        var tableGroups=new HashMap<String,Set<String>>();
        var dropGroups=new HashMap<Set<String>,Set<String>>();
        var signatures=new HashMap<String,Set<String>>();
        var parsedTables=new HashMap<String,Set<String>>();
        for(var entry:oreTables.entrySet()) {
            var drops=new HashSet<>(parsedTables.computeIfAbsent(entry.getValue(),key->{
                var values=new HashSet<String>();collect(tables.apply(key),tables,itemTags,new HashSet<>(Set.of(key)),values,0);return Set.copyOf(values);
            }));
            for(String drop:drops) byDrop.computeIfAbsent(drop,k->new HashSet<>()).add(entry.getKey());
            drops.removeAll(oreItems); // Silk Touch's own block must not split shallow/deep variants.
            Set<String> signature=Set.copyOf(drops);signatures.put(entry.getKey(),signature);
            if(!signature.isEmpty())dropGroups.computeIfAbsent(signature,k->new HashSet<>()).add(entry.getKey());
            if(!entry.getValue().equals("minecraft:empty"))tableGroups.computeIfAbsent(entry.getValue(),k->new HashSet<>()).add(entry.getKey());
        }
        for(var entry:oreTables.entrySet()) {
            var variants=new HashSet<String>();variants.add(entry.getKey());
            variants.addAll(tableGroups.getOrDefault(entry.getValue(),Set.of()));
            variants.addAll(dropGroups.getOrDefault(signatures.get(entry.getKey()),Set.of()));
            byOre.put(entry.getKey(),Set.copyOf(variants));
        }
        byDrop.replaceAll((key,value)->Set.copyOf(value));
    }
    public Map<String,Set<String>> dropSources(){return Map.copyOf(byDrop);}
    public Map<String,Set<String>> oreVariants(){return Map.copyOf(byOre);}
    public Set<String> select(String item, String oreBlock) {
        return oreBlock!=null && byOre.containsKey(oreBlock) ? byOre.get(oreBlock) : byDrop.getOrDefault(item,Set.of());
    }
    private static void collect(JsonElement value,Function<String,JsonElement> tables,Function<String,Set<String>> tags,
                                Set<String> visiting,Set<String> out,int depth) {
        if(value==null||depth>64)return;
        if(value.isJsonArray()){for(var child:value.getAsJsonArray())collect(child,tables,tags,visiting,out,depth+1);return;}
        if(!value.isJsonObject())return;
        var node=value.getAsJsonObject();String type=string(node,"type");var produced=new HashSet<String>();
        if("minecraft:item".equals(type))add(node,"name",produced);
        else if("minecraft:tag".equals(type)){String tag=string(node,"name");if(tag!=null)produced.addAll(tags.apply(tag));}
        else if("minecraft:loot_table".equals(type)) {
            var ref=node.has("value")?node.get("value"):node.get("name");
            if(ref!=null&&ref.isJsonPrimitive()) {
                String id=ref.getAsString();if(visiting.add(id)){collect(tables.apply(id),tables,tags,visiting,produced,depth+1);visiting.remove(id);}
            } else collect(ref,tables,tags,visiting,produced,depth+1);
        } else if(type!=null&&!type.startsWith("minecraft:")) {
            // Custom entry codecs commonly expose their resulting item under these fields.
            add(node,"item",produced);add(node,"name",produced);add(node,"result",produced);
        }
        for(String field:new String[]{"pools","entries","children"})collect(node.get(field),tables,tags,visiting,produced,depth+1);
        if(node.has("functions")&&node.get("functions").isJsonArray())for(var function:node.getAsJsonArray("functions"))if(function.isJsonObject()) {
            var f=function.getAsJsonObject();String name=string(f,"function");
            if("minecraft:set_item".equals(name)||name!=null&&!name.startsWith("minecraft:")) {
                if("minecraft:set_item".equals(name)&&(!f.has("conditions")||f.get("conditions").isJsonArray()&&f.getAsJsonArray("conditions").isEmpty()))produced.clear();
                add(f,"item",produced);add(f,"result",produced);
            }
        }
        out.addAll(produced);
        // Conditions (including match_tool predicates) describe inputs, not dropped items.
    }
    private static String string(JsonObject node,String field) {
        var value=node.get(field);return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()?value.getAsString():null;
    }
    private static void add(JsonObject node,String field,Set<String> out) {
        String id=string(node,field);
        if(id==null&&node.has(field)&&node.get(field).isJsonObject())id=string(node.getAsJsonObject(field),"id");
        if(id!=null){id=id.contains(":")?id:"minecraft:"+id;if(id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))out.add(id);}
    }
}
