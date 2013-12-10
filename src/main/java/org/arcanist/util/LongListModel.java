// Custom ArrayList model
// from Java Design Patterns for Long Lists
// http://jdj.sys-con.com/read/45842.htm

package org.arcanist.util;

import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;


public class LongListModel extends AbstractListModel {
  private List<Object> dataList;


  /**
   * Constructor.
   */
  public LongListModel() {
    dataList = new ArrayList<Object>();
  }

  /**
   * Constructor.
   *
   * @param size size of list
   */
  public LongListModel(int size) {
    dataList = new ArrayList<Object>(size);
  }


  /**
   * Returns specified object.
   *
   * @param i index
   * @return object
   */
  @Override
  public Object getElementAt(int i) {return dataList.get(i);}
  public Object elementAt(int i) {return getElementAt(i);}

  /**
   * Returns size of list.
   *
   * @return size
   */
  @Override
  public int getSize() {return dataList.size();}
  public int size() {return getSize();}

  /**
   * Adds the specified element to the end of the list.
   *
   * @param o Object
   */
  public void addElement(Object o) {
    dataList.add(o);
    fireIntervalAdded(this, dataList.size()-1, dataList.size()-1);
  }

  /**
   * Inserts the specified element at a given index.
   *
   * @param o Object
   */
  public void addElement(int i, Object o) {
    dataList.add(i, o);
    fireIntervalAdded(this, dataList.size()-1, dataList.size()-1);
  }

  /**
   * Removes the specified element.
   *
   * @param i index
   */
  public void removeElement(int i) {
    int size = dataList.size();
    dataList.remove(i);
    if (size > 0) {
      fireIntervalRemoved(this, i, i);
    }
  }

  /**
   * Adds all of the elements in the specified array to the end of the list.
   *
   * @param objects elements to be appended to the list
   */
  public void addAll(Object[] objects) {
    for(int i = 0; i < objects.length; i++)
      dataList.add(objects[i]);
    if (objects.length > 0)
      fireIntervalAdded(this, dataList.size()-objects.length, dataList.size()-1);
  }

  /**
   * Inserts all of the elements in the specified array starting at the specified position.
   *
   * @param index index at which to insert first element from the specified array
   * @param objects elements to be inserted into the list
   */
  public void addAll(int index, Object[] objects) {
    for(int i = 0; i < objects.length; i++)
      dataList.add(index+i, objects[i]);
    if (objects.length > 0)
      fireIntervalAdded(this, index, index+objects.length-1);
  }

  /**
   * Removes from the list all of the elements whose index is between fromIndex, inclusive and toIndex, exclusive.
   *
   * @param fromIndex low endpoint (inclusive) of the subList
   * @param toIndex high endpoint (exclusive) of the subList
   */
  public void removeRange(int fromIndex, int toIndex) {
    dataList.subList(fromIndex, toIndex).clear();
    fireIntervalRemoved(this, fromIndex, toIndex-1);;
  }

  /**
   * Removes all objects from the list.
   */
  public void clear() {
    int size = dataList.size();
    dataList.clear();
    if (size > 0) {
      fireIntervalRemoved(this, 0, size-1);
    }
  }

  /**
   * Returns list as array.
   *
   * @return list
   */
  public Object[] toArray() {
    return dataList.toArray(new Object[dataList.size()]);
  }
}
