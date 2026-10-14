package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.locator.ToolLocatorRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ToolLocatorEdges {
    private ToolLocatorEdges() {}
    public static void draw(PoseStack poses,VertexConsumer vertices,ToolLocatorClient.Visible target) {
        int color=switch(target.category()) {
            case ToolLocatorRules.STORAGE -> 0xFFFFFF00;
            case ToolLocatorRules.SPAWNER -> 0xFFB040FF;
            case ToolLocatorRules.ARCHAEOLOGY -> 0xFF00FF00;
            default -> 0xFFFFFFFF;
        };
        float x=-.002F,y=-.002F,z=-.002F,s=1.004F;
        for(int a=0;a<2;a++) for(int b=0;b<2;b++) {
            line(poses,vertices,x,y+a*s,z+b*s,x+s,y+a*s,z+b*s,color);
            line(poses,vertices,x+a*s,y,z+b*s,x+a*s,y+s,z+b*s,color);
            line(poses,vertices,x+a*s,y+b*s,z,x+a*s,y+b*s,z+s,color);
        }
    }
    private static void line(PoseStack poses,VertexConsumer vertices,float x1,float y1,float z1,float x2,float y2,float z2,int color) {
        vertices.addVertex(poses.last(),x1,y1,z1).setColor(color);
        vertices.addVertex(poses.last(),x2,y2,z2).setColor(color);
    }
}
