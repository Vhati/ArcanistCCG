package org.arcanist.client;

import org.arcanist.client.*;
import org.arcanist.util.*;


public interface CardListContainer {


  public void addCard(Card newCard, int index);

  public void addCard(Card newCard);

  public Card takeCard(int index);

  public Card getCard(int index);

  public int getCardCount();
}
