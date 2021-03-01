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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator
 *
 * @author Clinton Begin
 */
// LRU-最近最少使用淘汰，装饰器模式
public class LruCache implements Cache {

  //持有一个Cache的实现类
  private final Cache delegate;
  //用于实现LRU算法的LinkedHashMap
  private Map<Object, Object> keyMap;
  // 存储时间最长的key
  private Object eldestKey;

  //构造方法，使用的Cache接口的实现类必须传入。缓存大小设置为1024
  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  //设置size,在构造方法中被调用，为类属性keyMap(用于实现LRU算法)赋值(新创建一个LinkedHashMap)
  public void setSize(final int size) {
    //构造一个LinkedHashMap，size为参数size
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      //重写LinkedHashMap的removeEldestEntry()方法 删除最近最少使用的节点
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        //若keyMap(LinkedHashMap类型)中的节点数量已经大于设置的缓存大小
        boolean tooBig = size() > size;
        if (tooBig) {
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  @Override
  public void putObject(Object key, Object value) {
    //向装饰的Cache实现类中添加
    delegate.putObject(key, value);
    //向装饰的Cache缓存中添加，同时查看是否有最近最久未使用的key需要从Cache实现类中删除
    cycleKeyList(key);
  }

  @Override
  public Object getObject(Object key) {
    keyMap.get(key); //touch
    //从装饰的Cache实现类中获取并返回
    return delegate.getObject(key);
  }

  //移除 参数key对应的缓存节点
  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  //清除缓存
  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  //循环key列表
  private void cycleKeyList(Object key) {
    //向LinkedList中添加
    keyMap.put(key, key);
    //若最近最久未使用的key不为空，从装饰的Cache实现类中删除，再将最近最久未使用的key设置为空
    if (eldestKey != null) {
      delegate.removeObject(eldestKey);
      eldestKey = null;
    }
  }

}
