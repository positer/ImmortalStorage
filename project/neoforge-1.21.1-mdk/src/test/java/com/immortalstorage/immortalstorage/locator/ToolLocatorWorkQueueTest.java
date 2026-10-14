package com.immortalstorage.immortalstorage.locator;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class ToolLocatorWorkQueueTest {
    static class Task implements ToolLocatorWorkQueue.Task {
        int remaining,reads;boolean valid=true,finished,cancelled;
        Task(int remaining){this.remaining=remaining;}
        public boolean valid(){return valid;}
        public int advance(int budget){int read=Math.min(remaining,budget);remaining-=read;reads+=read;return read;}
        public boolean done(){return remaining==0;}
        public void finish(){finished=true;}
        public void cancel(){cancelled=true;}
    }
    @Test void idleAndCompletedQueuesNeverRepeatWorldReads() {
        var queue=new ToolLocatorWorkQueue();assertEquals(0,queue.drain(16_384));
        var task=new Task(100);queue.add(task);assertEquals(100,queue.drain(16_384));assertTrue(task.finished);
        for(int i=0;i<100;i++)assertEquals(0,queue.drain(16_384));assertEquals(100,task.reads);
    }
    @Test void sharedBudgetIsGlobalRatherThanMultipliedByPlayerCount() {
        var queue=new ToolLocatorWorkQueue();var tasks=new ArrayList<Task>();
        for(int i=0;i<10;i++){var task=new Task(10_000);tasks.add(task);queue.add(task);}
        assertEquals(4096,queue.drain(4096));assertEquals(4096,tasks.stream().mapToInt(t->t.reads).sum());
    }
    @Test void roundRobinLetsTheNextPlayerProgressBeforeTheFirstCompletes() {
        var queue=new ToolLocatorWorkQueue();var first=new Task(2000);var second=new Task(2000);queue.add(first);queue.add(second);
        queue.drain(512);assertEquals(512,first.reads);assertEquals(0,second.reads);
        queue.drain(512);assertEquals(512,first.reads);assertEquals(512,second.reads);
    }
    @Test void invalidatedChunkWorkIsDroppedWithoutReadingOrRecreatingAChunk() {
        var queue=new ToolLocatorWorkQueue();var task=new Task(4096);task.valid=false;queue.add(task);
        assertEquals(0,queue.drain(16_384));assertEquals(0,task.reads);assertTrue(task.cancelled);assertFalse(task.finished);
    }
    @Test void permissionRevokedBetweenSlicesStopsReadsWhileOtherPlayersContinue() {
        var queue=new ToolLocatorWorkQueue();var revoked=new Task(4096);var other=new Task(4096);
        queue.add(revoked);queue.add(other);queue.drain(512);
        revoked.valid=false;
        assertEquals(4096,queue.drain(16_384));
        assertEquals(512,revoked.reads);assertTrue(revoked.cancelled);assertFalse(revoked.finished);
        assertEquals(4096,other.reads);assertTrue(other.finished);
        assertEquals(0,queue.drain(16_384));
    }
}
