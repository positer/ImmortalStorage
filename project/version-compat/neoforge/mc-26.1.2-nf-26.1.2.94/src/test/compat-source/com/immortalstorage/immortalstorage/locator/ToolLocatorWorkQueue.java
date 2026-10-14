package com.immortalstorage.immortalstorage.locator;

import java.util.ArrayDeque;

/** Event-created work only; shared round-robin budgets prevent per-player scan spikes. */
public final class ToolLocatorWorkQueue {
    public interface Task {
        boolean valid();
        int advance(int budget);
        boolean done();
        void finish();
        void cancel();
    }
    private final ArrayDeque<Task> tasks=new ArrayDeque<>();
    public void add(Task task){tasks.addLast(task);}
    public void clear(){tasks.clear();}
    public int drain(int budget) {
        int used=0,visits=0;
        while(used<budget&&!tasks.isEmpty()&&visits++<512) {
            Task task=tasks.removeFirst();
            if(!task.valid()){task.cancel();continue;}
            int allowance=Math.min(512,budget-used),cost=task.advance(allowance);
            if(cost<0||cost>allowance)throw new IllegalStateException("locator task exceeded work budget");
            used+=cost;
            if(task.done())task.finish();else tasks.addLast(task);
        }
        return used;
    }
}
