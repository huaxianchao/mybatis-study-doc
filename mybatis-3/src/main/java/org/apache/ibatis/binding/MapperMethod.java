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

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
//MapperMethod方法，对Mapper中的Method方法进行一层封装
public class MapperMethod {

  private final SqlCommand command;
  private final MethodSignature method;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, method);
  }

  /** 实际sql语句的执行与结果获取，MapperProxy的invoke方法会调用该方法
   * @param: sqlSession
   * @param: args
   * @Return: java.lang.Object
   */
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    if (SqlCommandType.INSERT == command.getType()) {
      //获取运行时参数列表Map
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.insert(command.getName(), param));
    } else if (SqlCommandType.UPDATE == command.getType()) {
      //获取运行时参数列表Map
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.update(command.getName(), param));
    } else if (SqlCommandType.DELETE == command.getType()) {
      //获取运行时参数列表Map
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.delete(command.getName(), param));
    }
    //Select的判断较多，单独分析一下
    //若SQL节点类型是SELECT
    else if (SqlCommandType.SELECT == command.getType()) {
      //若mapper方法的返回值类型是void 且 方法有ResultHandler
      if (method.returnsVoid() && method.hasResultHandler()) {
        //用ResultHandler执行
        executeWithResultHandler(sqlSession, args);
        //返回null，因为返回值类型是void
        result = null;
      }
      //若 mapper方法返回多条记录 且 (mapper方法的返回值类型不是void 或 mapper方法没有解析器 )
      else if (method.returnsMany()) {
        //以返回多条记录的形式执行
        result = executeForMany(sqlSession, args);
      }
      //若mapper方法使用了@KeyMap注解，且(mapper方法的返回值类型不是void 或 mapper方法没有解析器 )
      else if (method.returnsMap()) {
        //以返回Map的形式执行
        result = executeForMap(sqlSession, args);
      }
      //兜底：若mapper方法 todo 判断条件用流程图能更好表达
      else {
        //获取运行时参数列表Map
        Object param = method.convertArgsToSqlCommandParam(args);
        //以selectOne的形式执行
        result = sqlSession.selectOne(command.getName(), param);
      }
    }
    //若节点类型是FLUSH
    else if (SqlCommandType.FLUSH == command.getType()) {
      //以刷新batchStatement（批处理）的形式执行
        result = sqlSession.flushStatements();
    }
    //兜底：若节点类型未在SqlCommandType枚举中匹配成功，抛出异常
    else {
      throw new BindingException("Unknown execution method for: " + command.getName());
    }
    //此时已经获取的sql的执行结果
    // tips：isPrimitive用于判断Class是否是原始类型(包括8种基本数据类型和void)
    //若执行结果为空 且 mapper方法的定义的返回值类型是基本类型或void 且 方法的返回值类型不是void，抛出异常
    //即方法定义了返回结果，但sql执行结果并没有获取到返回值
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName() 
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }

  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
      result = Integer.valueOf(rowCount);
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
      result = Long.valueOf(rowCount);
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
      result = Boolean.valueOf(rowCount > 0);
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  //使用ResultHandler执行
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName() 
          + " needs either a @ResultMap annotation, a @ResultType annotation," 
          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<E>selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }

  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  @SuppressWarnings("unchecked")
  private <E> E[] convertToArray(List<E> list) {
    E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
    array = list.toArray(array);
    return array;
  }

  //以返回Map的形式执行
  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    //获取运行时参数列表Map
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  public static class ParamMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -2212268410512043556L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
      }
      return super.get(key);
    }

  }

  //内部类，SQL命令类
  public static class SqlCommand {

    private final String name;
    //类型：枚举值，UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH;
    private final SqlCommandType type;

    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      String statementName = mapperInterface.getName() + "." + method.getName();
      MappedStatement ms = null;
      if (configuration.hasStatement(statementName)) {
        ms = configuration.getMappedStatement(statementName);
      } else if (!mapperInterface.equals(method.getDeclaringClass())) { // issue #35
        String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
        if (configuration.hasStatement(parentStatementName)) {
          ms = configuration.getMappedStatement(parentStatementName);
        }
      }
      if (ms == null) {
        if(method.getAnnotation(Flush.class) != null){
          name = null;
          type = SqlCommandType.FLUSH;
        } else {
          throw new BindingException("Invalid bound statement (not found): " + statementName);
        }
      } else {
        name = ms.getId();
        type = ms.getSqlCommandType();
        if (type == SqlCommandType.UNKNOWN) {
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }
  }

  //内部类，方法签名,对该方法的一些描述(返回值类型，参数等)
  public static class MethodSignature {

    //是否返回多条记录，当方法的返回值为集合或数组时->true
    private final boolean returnsMany;
    //是否返回Map（在方法上使用了@Mapkey注解后该值为true）
    private final boolean returnsMap;
    //方法的返回值类型是否为void
    private final boolean returnsVoid;
    //方法的返回值类型
    private final Class<?> returnType;
    //方法上的注解@MapKey中指定的值，若没有@Mapkey注解则为null
    private final String mapKey;
    //ResultHandler类型的参数在方法的新参列表中的下标，不存在时则为null
    private final Integer resultHandlerIndex;
    //RowBounds烈性的参数在方法的形参列表中的下标，不存在是->
    private final Integer rowBoundsIndex;
    //形参列表。key->参数下标，value->参数名(若定义了@Param则是@Param中指定的参数名)
    private final SortedMap<Integer, String> params;
    //方法参数上是否有@Param注解，该值用于判断是否需要处理参数别名
    private final boolean hasNamedParameters;

    public MethodSignature(Configuration configuration, Method method) {
      this.returnType = method.getReturnType();
      this.returnsVoid = void.class.equals(this.returnType);
      this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
      this.mapKey = getMapKey(method);
      this.returnsMap = (this.mapKey != null);
      this.hasNamedParameters = hasNamedParams(method);
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      this.params = Collections.unmodifiableSortedMap(getParams(method, this.hasNamedParameters));
    }

    /**把方法参数转化成SQL语句的参数
     * @param: args -> 实参列表
     * @Return: java.lang.Object
     */ 
    public Object convertArgsToSqlCommandParam(Object[] args) {
      //获取方法的形参列表的参数个数
      final int paramCount = params.size();
      //若实参列表为空或形参列表为空，返回null
      if (args == null || paramCount == 0) {
        return null;
      }
      //若形参列表的参数个数为1 且 形参列表中没有@Param注解
      else if (!hasNamedParameters && paramCount == 1) {
        //其实就是返回的实际参数的下标为0的参数
        return args[params.keySet().iterator().next().intValue()];
      }
      //若形参列表不为空&实参列表不为空& (形参列表长度超过1或形参列表中含有@param注解)
      else {
        //定义一个map，我自己称之为运行时参数列表Map，存放的元素为：
        // key->根据形参的参数名得出(@param注解指定参数名特殊处理过的)
        //value->实际运行时候传入的参数，且与key对应
        final Map<String, Object> param = new ParamMap<Object>();
        int i = 0;
        //对形参列表进行遍历（元素为Map.Entry节点）（附带fori循环的效果，i++）
        for (Map.Entry<Integer, String> entry : params.entrySet()) {
          //形参列表的参数名作为key，实参列表中对应下标的参数作为value，存放进运行时参数列表Map中
          param.put(entry.getValue(), args[entry.getKey().intValue()]);
          // issue #71, add param names as param1, param2...but ensure backward compatibility
          //生成一个统一参数名
          final String genericParamName = "param" + String.valueOf(i + 1);
          //若运行时参数列表Map中没有该统一参数名作为key的节点
          if (!param.containsKey(genericParamName)) {
            //将统一参数名作为key，实参列表中的对应下标的参数作为value，存进map
            param.put(genericParamName, args[entry.getKey()]);
          }
          i++;
        }
        //返回运行时参数列表Map
        return param;
      }
    }

    public boolean hasRowBounds() {
      return rowBoundsIndex != null;
    }

    public RowBounds extractRowBounds(Object[] args) {
      return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
    }

    public boolean hasResultHandler() {
      return resultHandlerIndex != null;
    }

    public ResultHandler extractResultHandler(Object[] args) {
      return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
    }

    public String getMapKey() {
      return mapKey;
    }

    public Class<?> getReturnType() {
      return returnType;
    }

    public boolean returnsMany() {
      return returnsMany;
    }

    public boolean returnsMap() {
      return returnsMap;
    }

    public boolean returnsVoid() {
      return returnsVoid;
    }

    /**根据参数类型，获取在参数列表中的唯一的参数下标，若找到多个会报错，若循环完毕未找到返回null
     * @param: method
     * @param: paramType
     * @Return: java.lang.Integer
     */
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      //获取当前方法的所有参数类型
      final Class<?>[] argTypes = method.getParameterTypes();
      //遍历获取到的所有参数类型
      for (int i = 0; i < argTypes.length; i++) {
        if (paramType.isAssignableFrom(argTypes[i])) {
          if (index == null) {
            index = i;
          } else {
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**获取方法上的@MapKey参数的值，若该方法没有@MapKey注解，返回null
     * @Author: xianchao.hua
     * @Date: 2021/1/14 9:58
     * @param: null
     * @Return: 
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      //若方法的返回值类型是Map
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        //获取方法上的@MapKey注解
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        //若当前方法上有@MapKey注解，则获取注解中指定的值
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }

    //把方法的参数处理为对应位置和别名，若
    private SortedMap<Integer, String> getParams(Method method, boolean hasNamedParameters) {
      //使用的是TreeMap
      final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
      //获取该方法的所有参数类型->Class类型的数组
      final Class<?>[] argTypes = method.getParameterTypes();
      //若该方法的所有参数类型遍历
      for (int i = 0; i < argTypes.length; i++) {
        //若参数中既没有传入RowBounds类型也没有传入ResultHandler类型
        //todo RowBounds和ResultHandler未分析
        if (!RowBounds.class.isAssignableFrom(argTypes[i]) && !ResultHandler.class.isAssignableFrom(argTypes[i])) {
          //下面是对返回值中的value进行处理
          //默认设置param(返回值map中value)为当前循环中map的size，即0->1->2->3(每次循环递增)
          String paramName = String.valueOf(params.size());
          //若参数中存在@Param注解
          if (hasNamedParameters) {
            //对参数进行@Param注解处理
            //这行代码这里是不知道当前次循环处理的参数是否包含@Param参数的
            paramName = getParamNameFromAnnotation(method, i, paramName);
          }
          //TreeMap::put -> key是当前参数下标，value是形参名（若使用@param则是@param注解指定的别命）
          params.put(i, paramName);
        }
      }
      return params;
    }

    /**获取@param注解中指定的参数名
     * @param: method  要处理的方法
     * @param: i  当前需要处理的参数在方法参数列表中的下标
     * @param: paramName 默认返回的paramName(若当前处理的参数中不包含@Param注解，则返回该值)
     * @Return: java.lang.String
     */
    private String getParamNameFromAnnotation(Method method, int i, String paramName) {
      //获取方法中 下标为 i的参数上的所有注解
      final Object[] paramAnnos = method.getParameterAnnotations()[i];
      //遍历该参数上的所有注解
      for (Object paramAnno : paramAnnos) {
        //若当前处理的参数的注解中包含@Param
        if (paramAnno instanceof Param) {
          //设置param为@Param注解中的value属性
          paramName = ((Param) paramAnno).value();
          break;
        }
      }
      return paramName;
    }

    //判断方法参数上面是否有@Param注解，只要有一个@Param注解就返回true，一个都没有时返回false
    private boolean hasNamedParams(Method method) {
      //每个方法参数可以有多个注解，所以这里定义为二维数组
      final Object[][] paramAnnos = method.getParameterAnnotations();
      for (Object[] paramAnno : paramAnnos) {
        for (Object aParamAnno : paramAnno) {
          if (aParamAnno instanceof Param) {
            return true;
          }
        }
      }
      return false;
    }

  }

}
