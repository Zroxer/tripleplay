//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.util;

import playn.core.PlayN;

/**
 * Handles execution of actions after a specified delay.
 */
public class Timer
{
    /** A handle on registered actions that can be used to cancel them. */
    public static interface Handle {
        /** Cancels the action in question. */
        void cancel ();
    }

    /** Creates a timer instance that can be used to schedule actions.
     * Don't forget to call {@link #update} on it, every frame. */
    public Timer () {
        this(System.currentTimeMillis());
    }

    /** Executes the supplied action after the specified number of milliseconds have elapsed.
     * @return a handle that can be used to cancel the execution of the action. */
    public Handle after (int millis, Runnable action) {
        return add(millis, 0, action);
    }

    /** Executes the supplied action starting {@code millis} from now and every {@code millis}
     * thereafter.
     * @return a handle that can be used to cancel the execution of the action. */
    public Handle every (int millis, Runnable action) {
        return atThenEvery(millis, millis, action);
    }

    /** Executes the supplied action starting {@code initialMillis} from now and every {@code
     * repeatMillis} there after.
     * @return a handle that can be used to cancel the execution of the action. */
    public Handle atThenEvery (int initialMillis, int repeatMillis, Runnable action) {
        return add(initialMillis, repeatMillis, action);
    }

    /** This should be called from {@link Game#update}, or similar. */
    public void update () {
        update(System.currentTimeMillis());
    }

    // this and update(long) exist so that we can unit test this class
    protected Timer (long now) {
        _currentTime = now;
    }

    protected void update (long now) {
        _currentTime = now;

        Action root = _root;
        while (root.next != null && root.next.nextExpire <= now) {
            Action act = root.next;
            try {
                act.action.run();
            } catch (Exception e) {
                PlayN.log().warn("Action failed", e);
            }
            if (!act.cancelled()) {
                if (act.repeatMillis == 0) {
                    act.cancel();
                } else {
                    act.nextExpire += act.repeatMillis;
                    root.next = insert(act, act.next);
                }
            }
        }
    }

    protected Handle add (int initialMillis, int repeatMillis, Runnable action) {
        Action act = new Action(initialMillis, repeatMillis, action);
        _root.next = insert(act, _root.next);
        return act;
    }

    protected Action insert (Action target, Action next) {
        if (next == null || next.nextExpire > target.nextExpire) {
            target.next = next;
            return target;
        } else {
            next.next = insert(target, next.next);
            return next;
        }
    }

    protected Action remove (Action target, Action next) {
        if (target == next) return target.next;
        else if (next == null) return null;
        else return remove(target, next.next);
    }

    protected class Action implements Handle {
        public final int repeatMillis;
        public final Runnable action;

        public long nextExpire;
        public Action next;

        public Action (int initialMillis, int repeatMillis, Runnable action) {
            this.nextExpire = _currentTime + initialMillis;
            this.repeatMillis = repeatMillis;
            this.action = action;
        }

        public boolean cancelled () {
            return nextExpire == -1;
        }

        @Override public void cancel () {
            if (!cancelled()) {
                _root.next = remove(this, _root.next);
                nextExpire = -1;
                next = null;
            }
        }

        @Override public String toString () {
            return nextExpire + "/" + repeatMillis + "/" + action + " -> " + next;
        }
    }

    protected final Action _root = new Action(0, 0, null);
    protected long _currentTime;
}