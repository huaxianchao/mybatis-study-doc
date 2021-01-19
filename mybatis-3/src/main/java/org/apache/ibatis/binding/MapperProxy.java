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
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
//Mapper方法代理，基于jdk的动态代理
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  private final SqlSession sqlSession;
  private final Class<T> mapperInterface;
  /**方法缓存Map，key->mapper方法，value->包装并解析的MapperMethod方法
   * 因为Mapper方法解析包装成对应MapperMethod对象也很复杂耗时，所以做成了缓存
   * {@link MapperProxyFactory}在调用该方法时传入，在MapperPrxyFactory中统一维护，
   * 同一个mapper接口下的MapperProxy使用的是指向同一个地址的缓存
   */ 
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    //若调用的是从基类Object中自动继承来的方法，直接调用无需代理
    if (Object.class.equals(method.getDeclaringClass())) {
      try {
        return method.invoke(this, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
    //这里根据mapper接口的方法从缓存的Map中获取MapperMethod方法，若第一次调用（即程序启动时），则根据Mapper对象生成MapperMethod对象并放入缓存
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    //调用实际的MapperMethod对象的方法
    return mapperMethod.execute(sqlSession, args);
  }

  /** 根据Mapper方法从缓存Map中获取对应的MapperMethod方法
   *  若未获取到，则把method包装成MappedMethod对象并存入缓存Map中并返回
   * @param: method
   * @Return: org.apache.ibatis.binding.MapperMethod
   */
  private MapperMethod cachedMapperMethod(Method method) {
    //先从methodCache中获取，查看该方法是否已经缓存
    MapperMethod mapperMethod = methodCache.get(method);
    if (mapperMethod == null) {
      //若获取结果为空，将参数的Method对象包装成MapperMethod对象并存放到methodCache中
      mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
      methodCache.put(method, mapperMethod);
    }
    //若成功从缓存中获取到，直接返回
    return mapperMethod;
  }

}
