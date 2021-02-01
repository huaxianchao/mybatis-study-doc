/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Clinton Begin
 */
//缓存的Key，实现了Cloneable方法，使用深拷贝模式
public class CacheKey implements Cloneable, Serializable {

  private static final long serialVersionUID = 1146682552656046210L;

  public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

  private static final int DEFAULT_MULTIPLYER = 37;
  private static final int DEFAULT_HASHCODE = 17;

  private int multiplier;
  private int hashcode;
  private long checksum;
  private int count;
  //更新列表
  private List<Object> updateList;

  public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<Object>();
  }

  public CacheKey(Object[] objects) {
    this();
    updateAll(objects);
  }

  public int getUpdateCount() {
    return updateList.size();
  }

  //更新
  public void update(Object object) {
    //若参数不为空 且 参数是数组类型
    if (object != null && object.getClass().isArray()) {
      //遍历数组
      int length = Array.getLength(object);
      for (int i = 0; i < length; i++) {
        Object element = Array.get(object, i);
        doUpdate(element);
      }
    } else {
      doUpdate(object);
    }
  }

  private void doUpdate(Object object) {
    //计算 局部变量baseHashCode
    //若 object ==null baseHashCode=1,否则baseHashCode = 对象的hashCode
    int baseHashCode = object == null ? 1 : object.hashCode();

    count++;
    checksum += baseHashCode;
    baseHashCode *= count;

    hashcode = multiplier * hashcode + baseHashCode;

    updateList.add(object);
  }

  public void updateAll(Object[] objects) {
    for (Object o : objects) {
      update(o);
    }
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof CacheKey)) {
      return false;
    }

    final CacheKey cacheKey = (CacheKey) object;

    if (hashcode != cacheKey.hashcode) {
      return false;
    }
    if (checksum != cacheKey.checksum) {
      return false;
    }
    if (count != cacheKey.count) {
      return false;
    }

    for (int i = 0; i < updateList.size(); i++) {
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (thisObject == null) {
        if (thatObject != null) {
          return false;
        }
      } else {
        if (!thisObject.equals(thatObject)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
    for (int i = 0; i < updateList.size(); i++) {
      returnValue.append(':').append(updateList.get(i));
    }

    return returnValue.toString();
  }


  //深拷贝模式，重写clone方法
  @Override
  public CacheKey clone() throws CloneNotSupportedException {
    //调用父类的clone方法
    CacheKey clonedCacheKey = (CacheKey) super.clone();
    //将引用属性ypdateList进行深拷贝
    clonedCacheKey.updateList = new ArrayList<Object>(updateList);
    return clonedCacheKey;
  }

}
