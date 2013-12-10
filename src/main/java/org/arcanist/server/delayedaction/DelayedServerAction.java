package org.arcanist.server.delayedaction;

import java.util.ArrayList;
import java.util.List;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


public abstract class DelayedServerAction {

  public static final int NONE = -1;
  public static final int MOOT = 0;
  public static final int WAIT = 1;
  public static final int EXPIRED = 2;
  public static final int DONE = 3;
  public static final int FAILED = 4;

  private long timeoutStart = System.currentTimeMillis();
  private long timeout = -1;

  private DelayedActionGroup actionGroup = null;

  // Integers of ids relevant to this action
  private List<Integer> localPlayerIds = new ArrayList<Integer>();


  public DelayedServerAction() {}


  /**
   * Init called once prior to the repeated checking.
   */
  public void init(ServerPlayerInfo[] spsArray, ServerThread st) {
    initTimeout();
  }

  /**
   * Cleanup called once after the action's final check.
   */
  public void cleanup(ServerPlayerInfo[] spsArray, ServerThread st) {}


  /**
   * Checks if the action should trigger.
   *
   * @param spsArray
   * @param st
   * @return MOOT     - This action is no longer relevant and should be removed.<br />
   *         EXPIRED  - This action took to long, so it aborted.<br />
   *         WAIT     - This action is not yet satisfied.<br />
   *         DONE     - This action has completed.<br />
   *         FAILED   - This action has failed to some degree.<br />
   *         RETRY    - This action has failed in a possibly recoverable way.
   */
  abstract int check(ServerPlayerInfo[] spsArray, ServerThread st);


  /**
   * Sets a timeout.
   * This is the maximum difference in time between the
   * moment initTimeout() was called and the current time
   * fed into isTimedOut() before it returns true. It is
   * up to subclasses whether timeouts are considered in
   * their check().
   *
   * @param n the new timeout, use a negative value for none
   */
  public void setTimeout(long n) {timeout = n;}
  public void initTimeout() {timeoutStart = System.currentTimeMillis();}
  public long getTimeout() {return timeout;}
  public boolean isTimedOut(long now) {
    if (timeout < 0 || now < 0) return false;
    return (now - timeoutStart > timeout);
  }


  public void setActionGroup(DelayedActionGroup ag) {
    actionGroup = ag;
  }
  public DelayedActionGroup getActionGroup() {
    return actionGroup;
  }

  /**
   * Get the backend ArrayList of local player id Integers.
   */
  public List<Integer> getLocalPlayerIds() {
    return localPlayerIds;
  }
  public void addLocalPlayerId(int id) {
    Integer tmpId = new Integer(id);
    if (!localPlayerIds.contains(tmpId)) localPlayerIds.add(tmpId);
  }
  /**
   * Batch adds new player ids from an ArrayList of Integers.
   */
  public void addLocalPlayerId(List<Integer> ids) {
    for (int i=0; i < ids.size(); i++) {
      Integer tmpId = ids.get(i);
      if (!localPlayerIds.contains(tmpId)) localPlayerIds.add(tmpId);
    }
  }
  public void removeLocalPlayerId(int id) {
    localPlayerIds.remove(new Integer(id));
  }
  /**
   * Returns true if a player id is known.
   * If this action has no local ids, its DelayedActionGroup is asked.
   * Otherwise returns true.
   */
  public boolean isLocalPlayerId(int id) {
    if (localPlayerIds.size() > 0)
      return localPlayerIds.contains(new Integer(id));
    else if (getActionGroup() != null)
      return getActionGroup().isLocalPlayerId(id);
    else
      return true;
  }
}
