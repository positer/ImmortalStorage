package com.immortalstorage.immortalstorage.compressor;

import com.immortalstorage.immortalstorage.client.render.HandbookPreviewCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandbookPreviewCameraTest {
    @Test void fullStructureCentreStaysAtTheViewportCentreAtEveryYawAndGuiScale() {
        check(-1,1,-1,1,3,1);
    }
    @Test void singleLayersCentreWithoutDependingOnTheAnchorHeight() {
        check(-1,1,-1,1,1,1);
    }
    private void check(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        for (int guiScale=1;guiScale<=4;guiScale++) for(int yaw=0;yaw<360;yaw+=30) {
            Matrix4f camera=new Matrix4f().scale(20F).rotateZ((float)Math.PI)
                    .rotateX((float)Math.toRadians(-30)).rotateY((float)Math.toRadians(yaw));
            Matrix4f centred=HandbookPreviewCamera.centre(camera,minX,minY,minZ,maxX,maxY,maxZ,99);
            Matrix4f nativePip=new Matrix4f().translate(99*guiScale/2F,99*guiScale,0)
                    .scale(guiScale,guiScale,-guiScale).mul(centred);
            // Include both native pos.getCenter() and the baked [0,1] quad centre.
            Vector3f origin=nativePip.transformPosition(new Vector3f((minX+maxX+2)/2F,
                    (minY+maxY+2)/2F,(minZ+maxZ+2)/2F));
            assertEquals(99*guiScale/2F,origin.x,0.001);
            assertEquals(99*guiScale/2F,origin.y,0.001);
        }
    }
}
