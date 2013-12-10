package org.arcanist.server.delayedaction;

import java.util.ArrayList;
import java.util.List;

import org.arcanist.server.delayedaction.*;
import org.arcanist.server.*;
import org.arcanist.util.*;


public class DelayedActionGroup {

  private DelayedServerAction nextAction = null;
  private int currentActionIndex = -1;
  private int lastResult = DelayedServerAction.NONE;
  private List<DelayedServerAction> actions = new ArrayList<DelayedServerAction>();

  // Integers of ids relevant to this action
  private List<Integer> localPlayerIds = new ArrayList<Integer>();


  public DelayedActionGroup() {}


  /**
   * Removes all nested actions.
   */
  public void clearActions() {
    nextAction = null;
    currentActionIndex = -1;
    lastResult = DelayedServerAction.NONE;
    actions.clear();
  }

  /**
   * Adds a nested action.
   */
  public void addAction(DelayedServerAction sa) {
    sa.setActionGroup(this);
    actions.add(sa);
    if (currentActionIndex < 0) currentActionIndex = 0;
  }

  /**
   * Removes a nested action.
   */
  public void removeAction(DelayedServerAction sa) {
    int removedIndex = getActionIndex(sa);
    if (removedIndex == -1) return;
    actions.remove(removedIndex);
    if (currentActionIndex > removedIndex) currentActionIndex--;
    if (nextAction == sa) nextAction = null;
  }

  /**
   * Returns the index of a nested action, or -1.
   */
  public int getActionIndex(DelayedServerAction sa) {
    for (int i=0; i < actions.size(); i++) {
      if (sa == actions.get(i)) return i;
    }
    return -1;
  }

  /**
   * Returns the current action, or null if empty/complete.
   */
  public DelayedServerAction getCurrentAction() {
    if (currentActionIndex < 0 || currentActionIndex >= actions.size()) {
      return null;
    }
    return (DelayedServerAction)actions.get(currentActionIndex);
  }

  /**
   * Ensures a given nested action is started next.
   */
  public void setNextAction(DelayedServerAction sa) {
    int index = getActionIndex(sa);
    if (index == -1) return;
    nextAction = sa;
  }

  /**
   * Moves onto the next action.
   * Calls the current action's cleanup() and the next one's init().
   * The current index will be incremented unless setNextAction() was called.
   */
  public void nextAction(ServerPlayerInfo[] spsArray, ServerThread st) {
    DelayedServerAction currentAction = getCurrentAction();
    if (currentAction != null) currentAction.cleanup(spsArray, st);

    if (nextAction != null) {
      currentActionIndex = getActionIndex(nextAction);
      nextAction = null;
    }
    else currentActionIndex++;

    currentAction = getCurrentAction();
    if (currentAction != null) {
      st.logMessage(LogManager.INFO_LEVEL, "Init DelayedServerAction "+ currentAction.getClass().getName().replaceAll(".*[.]", "") +".");
      currentAction.init(spsArray, st);
    }
  }

  /**
   * Returns the result of the last actions check().
   */
  public int getLastResult() {
    return lastResult;
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
  public void removeLocalPlayerId(int id) {
    localPlayerIds.remove(new Integer(id));
  }
  public boolean isLocalPlayerId(int id) {
    if (localPlayerIds.size() == 0) return true;
    else return localPlayerIds.contains(new Integer(id));
  }


  /**
   * Checks the current nested action.
   * Synchronize the ServerThread's stateMap first.
   * A return value of WAIT means the group needs no
   * further checking for now. NONE means the group
   * has run out of actions. If the nested action
   * returns anything other than WAIT, the value of
   * getLastResult() will be updated.
   *
   * @return the result of the check, or DelayedServerAction.NONE
   */
  public int check(ServerPlayerInfo[] spsArray, ServerThread st) {
    DelayedServerAction currentAction = getCurrentAction();
    if (currentAction == null) return DelayedServerAction.NONE;

    int checkResult = currentAction.check(spsArray, st);
    if (checkResult != DelayedServerAction.WAIT) lastResult = checkResult;
    return checkResult;
  }
}
