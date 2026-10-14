package com.immortalstorage.immortalstorage.locator;

import java.util.*;

/** Routes updates only to the owners of nearby windows, with dimension identity isolation. */
public final class ToolLocatorWatchIndex<D> {
    private record Attachment<D>(D dimension, Set<Long> chunks) {}
    private final Map<D,Map<Long,Set<UUID>>> dimensions=new IdentityHashMap<>();
    private final Map<UUID,Attachment<D>> owners=new HashMap<>();
    public void watch(UUID owner,D dimension,Collection<Long> chunks) {
        remove(owner);
        var unique=Set.copyOf(chunks);owners.put(owner,new Attachment<>(dimension,unique));
        var index=dimensions.computeIfAbsent(dimension,ignored->new HashMap<>());
        for(long chunk:unique) index.computeIfAbsent(chunk,ignored->new HashSet<>()).add(owner);
    }
    public void remove(UUID owner) {
        var attachment=owners.remove(owner);if(attachment==null)return;
        var index=dimensions.get(attachment.dimension());
        for(long chunk:attachment.chunks()) {
            var recipients=index.get(chunk);recipients.remove(owner);
            if(recipients.isEmpty())index.remove(chunk);
        }
        if(index.isEmpty())dimensions.remove(attachment.dimension());
    }
    public Set<UUID> nearby(D dimension,long chunk) {
        var index=dimensions.get(dimension);if(index==null)return Set.of();
        return Set.copyOf(index.getOrDefault(chunk,Set.of()));
    }
    public void clear(){dimensions.clear();owners.clear();}
}
