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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 */
//属性标记器
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {
  //当前标记器的名称
  private String name;
  //索引名称
  private String indexedName;
  //索引
  private String index;
  //子标记器的名称
  private String children;

  //构造方法
  public PropertyTokenizer(String fullname) {
    //查看参数中是否包含 '.'
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      //若包含 .
      //截取 .之前的字符串作为name
      name = fullname.substring(0, delim);
      //截取 .之后的字符串作为子标记器的name
      children = fullname.substring(delim + 1);
    } else {
      //若不包含 .,参数作为当前标记器名称，子标记器名称为null
      name = fullname;
      children = null;
    }
    //index 同 name
    indexedName = name;
    //查看是否包含 [
    delim = name.indexOf('[');
    //若包含 [
    if (delim > -1) {
      //截取 [ 之后，长度-1(其实就是]之前) 的字符串作为index
      index = name.substring(delim + 1, name.length() - 1);
      //截取 [ 之前的字符串作为name
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  //是否有子属性
  @Override
  public boolean hasNext() {
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }

  @Override
  public Iterator<PropertyTokenizer> iterator() {
    return this;
  }
}
