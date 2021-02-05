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
package org.apache.ibatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 */
//拦截器链 -- 责任链模式
public class InterceptorChain {

  //存放所有拦截器(插件)的集合
  private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

  /**在{@link org.apache.ibatis.session.Configuration}使用工厂模式创建E
   *  xecutor,StatementHandler,ParameterHandler，Resulthandler时被调用
   * 用于扩展mybatis
   * @param: target
   * @Return: java.lang.Object
   */
  public Object pluginAll(Object target) {
    //遍历所有拦截器，每个拦截器对方法进行处理(是否处理由拦截器自己决定)
    for (Interceptor interceptor : interceptors) {
      target = interceptor.plugin(target);
    }
    return target;
  }

  //添加拦截器（插件）
  public void addInterceptor(Interceptor interceptor) {
    interceptors.add(interceptor);
  }
  //获取所有拦截器（插件）
  public List<Interceptor> getInterceptors() {
    return Collections.unmodifiableList(interceptors);
  }

}
