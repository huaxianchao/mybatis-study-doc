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
package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 * 用于存储sql的分页信息，只是存储了SQL中limit的两个参数，具体计算和处理的方法不在这里
 */
public class RowBounds {

  //默认无分页

  //默认的无分页的左偏移量为0
  public static final int NO_ROW_OFFSET = 0;
  //默认取的最大条数是int类型的最大值
  public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;

  public static final RowBounds DEFAULT = new RowBounds();

  private int offset;
  private int limit;


  //无分页的构造方法
  public RowBounds() {
    this.offset = NO_ROW_OFFSET;
    this.limit = NO_ROW_LIMIT;
  }

  //有分页的构造方法
  public RowBounds(int offset, int limit) {
    this.offset = offset;
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public int getLimit() {
    return limit;
  }

}
